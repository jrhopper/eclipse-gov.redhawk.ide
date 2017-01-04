/*******************************************************************************
 * This file is protected by Copyright. 
 * Please refer to the COPYRIGHT file distributed with this source distribution.
 *
 * This file is part of REDHAWK IDE.
 *
 * All rights reserved.  This program and the accompanying materials are made available under 
 * the terms of the Eclipse Public License v1.0 which accompanies this distribution, and is available at 
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package gov.redhawk.ide.debug.internal;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.Notifier;
import org.eclipse.emf.common.notify.impl.AdapterImpl;
import org.eclipse.emf.ecore.EObject;
import org.omg.CORBA.ORB;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ServantNotActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

import CF.FileSystem;
import CF.FileSystemHelper;
import CF.FileSystemPOA;
import CF.FileSystemPOATie;
import CF.ResourceFactoryOperations;
import gov.redhawk.core.filemanager.filesystem.JavaFileSystem;
import gov.redhawk.core.resourcefactory.AbstractResourceFactoryProvider;
import gov.redhawk.core.resourcefactory.ComponentDesc;
import gov.redhawk.core.resourcefactory.ResourceDesc;
import gov.redhawk.core.resourcefactory.ResourceFactoryPlugin;
import gov.redhawk.ide.debug.SpdResourceFactory;
import gov.redhawk.ide.debug.ui.ScaDebugUiPlugin;
import gov.redhawk.ide.sdr.SdrPackage;
import gov.redhawk.ide.sdr.SdrRoot;
import gov.redhawk.ide.sdr.ui.SdrUiPlugin;
import gov.redhawk.model.sca.commands.ScaModelCommand;
import gov.redhawk.sca.util.MutexRule;
import gov.redhawk.sca.util.OrbSession;
import mil.jpeojtrs.sca.spd.SoftPkg;

/**
 * Provides descriptions of resources in the SDRROOT which can be launched in the sandbox.
 */
public class SdrResourceFactoryProvider extends AbstractResourceFactoryProvider {

	private static final MutexRule RULE = new MutexRule(SdrResourceFactoryProvider.class);
	private static final String SDR_CATEGORY = "SDR";
	private static final String DEPS_DIR = "/deps";

	private class SPDListener extends AdapterImpl {

		@Override
		public void notifyChanged(final org.eclipse.emf.common.notify.Notification msg) {
			if (disposed) {
				if (msg.getNotifier() instanceof Notifier) {
					((Notifier) msg.getNotifier()).eAdapters().remove(this);
				}
				return;
			}
			if (msg.getFeature() == SdrPackage.Literals.SOFT_PKG_REGISTRY__COMPONENTS) {
				switch (msg.getEventType()) {
				case Notification.ADD:
					addResource((SoftPkg) msg.getNewValue(), SpdResourceFactory.createResourceFactory((SoftPkg) msg.getNewValue()));
					break;
				case Notification.ADD_MANY:
					for (final Object obj : (Collection< ? >) msg.getNewValue()) {
						addResource((SoftPkg) obj, SpdResourceFactory.createResourceFactory((SoftPkg) obj));
					}
					break;
				case Notification.REMOVE:
					removeResource((SoftPkg) msg.getOldValue());
					break;
				case Notification.REMOVE_MANY:
					for (final Object obj : (Collection< ? >) msg.getOldValue()) {
						removeResource((SoftPkg) obj);
					}
					break;
				default:
					break;
				}
			}
		}
	};

	private OrbSession session;
	private final Map<EObject, ResourceDesc> resourceMap = Collections.synchronizedMap(new HashMap<EObject, ResourceDesc>());
	private SdrRoot root;
	private SPDListener componentsListener;
	private SPDListener devicesListener;
	private SPDListener serviceListener;
	private boolean disposed;

	public SdrResourceFactoryProvider() {
		SdrUiPlugin plugin = SdrUiPlugin.getDefault();
		this.root = plugin.getTargetSdrRoot();
		if (this.root == null) {
			return;
		}

		IPath domPath = plugin.getTargetSdrDomPath();
		if (domPath != null) {
			addVirtualMount(domPath.append(DEPS_DIR), DEPS_DIR);
		}

		this.componentsListener = new SPDListener();
		this.devicesListener = new SPDListener();
		this.serviceListener = new SPDListener();
		ScaModelCommand.execute(this.root, new ScaModelCommand() {

			@Override
			public void execute() {
				for (final SoftPkg spd : SdrResourceFactoryProvider.this.root.getComponentsContainer().getComponents()) {
					addResource(spd, SpdResourceFactory.createResourceFactory(spd));
				}
				for (final SoftPkg spd : SdrResourceFactoryProvider.this.root.getDevicesContainer().getComponents()) {
					addResource(spd, SpdResourceFactory.createResourceFactory(spd));
				}
				for (final SoftPkg spd : SdrResourceFactoryProvider.this.root.getServicesContainer().getComponents()) {
					addResource(spd, SpdResourceFactory.createResourceFactory(spd));
				}
				SdrResourceFactoryProvider.this.root.getComponentsContainer().eAdapters().add(SdrResourceFactoryProvider.this.componentsListener);
				SdrResourceFactoryProvider.this.root.getDevicesContainer().eAdapters().add(SdrResourceFactoryProvider.this.devicesListener);
				SdrResourceFactoryProvider.this.root.getServicesContainer().eAdapters().add(SdrResourceFactoryProvider.this.serviceListener);
			}
		});
	}

	/**
	 * Adds a virtual mount for a location in the file system
	 * @throws CoreException
	 */
	private void addVirtualMount(IPath sourceDir, String mountLocation) {
		// Ignore if file doesn't exist
		File dir = sourceDir.toFile();
		if (!dir.exists()) {
			return;
		}

		if (session == null) {
			session = OrbSession.createSession(ResourceFactoryPlugin.ID);
		}
		ORB orb = session.getOrb();
		POA poa;
		try {
			poa = session.getPOA();
		} catch (CoreException e) {
			ScaDebugUiPlugin.log(e);
			return;
		}
		FileSystemPOA fsPoa = new FileSystemPOATie(new JavaFileSystem(orb, poa, dir));
		try {
			FileSystem domDepsFs = FileSystemHelper.narrow(poa.servant_to_reference(fsPoa));
			addFileSystemMount(domDepsFs, mountLocation);
		} catch (ServantNotActive | WrongPolicy e) {
			ScaDebugUiPlugin.log(new Status(IStatus.ERROR, ScaDebugUiPlugin.PLUGIN_ID, "Unable to create virtual mount " + mountLocation, e));
		}
	}

	private void addResource(final SoftPkg spd, final ResourceFactoryOperations factory) {
		ComponentDesc desc = new ComponentDesc(spd, factory);
		desc.setCategory(SDR_CATEGORY);
		SdrResourceFactoryProvider.this.resourceMap.put(spd, desc);
		addResourceDesc(desc);
	}

	private void removeResource(final EObject resource) {
		final ResourceDesc desc = this.resourceMap.get(resource);
		if (desc != null) {
			removeResourceDesc(desc);
		}
	}

	@Override
	public void dispose() {
		Job.getJobManager().beginRule(RULE, null);
		try {
			if (disposed) {
				return;
			}
			disposed = true;
		} finally {
			Job.getJobManager().endRule(RULE);
		}

		// Stop listening for changes
		ScaModelCommand.execute(this.root, new ScaModelCommand() {
			@Override
			public void execute() {
				root.getComponentsContainer().eAdapters().remove(componentsListener);
				root.getDevicesContainer().eAdapters().remove(devicesListener);
				root.getServicesContainer().eAdapters().remove(serviceListener);
			}
		});
		this.root = null;

		// Remove resource descriptions
		synchronized (this.resourceMap) {
			for (final ResourceDesc desc : this.resourceMap.values()) {
				removeResourceDesc(desc);
			}
			this.resourceMap.clear();
		}

		// Remove file system mounts
		removeFileSystemMount(DEPS_DIR);

		// Dispose session
		if (session != null) {
			session.dispose();
			session = null;
		}
	}
}
