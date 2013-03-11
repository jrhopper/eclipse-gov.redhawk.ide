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

package gov.redhawk.ide.dcd.ui.wizard;

import gov.redhawk.ide.codegen.CodegenUtil;
import gov.redhawk.ide.codegen.util.ProjectCreator;
import gov.redhawk.ide.dcd.generator.newnode.NodeProjectCreator;
import gov.redhawk.ide.dcd.internal.ui.editor.NodeEditor;
import gov.redhawk.ide.dcd.ui.DcdUiActivator;
import gov.redhawk.ide.sdr.SdrRoot;
import gov.redhawk.ide.sdr.ui.SdrUiPlugin;

import java.lang.reflect.InvocationTargetException;

import mil.jpeojtrs.sca.spd.SoftPkg;
import mil.jpeojtrs.sca.util.DceUuidUtil;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExecutableExtension;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.gmf.runtime.diagram.ui.internal.properties.WorkspaceViewerProperties;
import org.eclipse.gmf.runtime.diagram.ui.parts.DiagramGraphicalViewer;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.statushandlers.StatusManager;
import org.eclipse.ui.wizards.newresource.BasicNewProjectResourceWizard;

/**
 * The Class NewScaDeviceProjectWizard.
 */
@SuppressWarnings("restriction")
public class NewScaNodeProjectWizard extends Wizard implements INewWizard, IExecutableExtension {

	protected static final long SDR_REFRESH_DELAY = 500;

	/** The node properties page. */
	private ScaNodeProjectPropertiesWizardPage nodePropertiesPage;

	/** The node properties page. */
	private ScaNodeProjectDevicesWizardPage nodeDevicesPage;

	private IFile openEditorOn;

	private IConfigurationElement fConfig;

	public NewScaNodeProjectWizard() {
		setWindowTitle("Node Project");
		setNeedsProgressMonitor(true);
	}

	@Override
	public boolean canFinish() {
		if (this.nodePropertiesPage.getContentsGroup().isCreateNewResource()) {
			return super.canFinish();
		} else {
			return this.nodePropertiesPage.isPageComplete();
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean performFinish() {
		try {
			// Find the working sets and where the new project should be located on disk
			final IWorkingSet[] workingSets = this.nodePropertiesPage.getSelectedWorkingSets();
			final boolean isCreateNewResource = this.nodePropertiesPage.getContentsGroup().isCreateNewResource();
			final String projectName = this.nodePropertiesPage.getProjectName();
			final java.net.URI locationURI;
			if (this.nodePropertiesPage.useDefaults()) {
				locationURI = null;
			} else {
				locationURI = this.nodePropertiesPage.getLocationURI();
			}
			final boolean generateId = this.nodePropertiesPage.getIdGroup().isGenerateId();
			final String providedId = this.nodePropertiesPage.getIdGroup().getProvidedId();
			final IPath existingDcdPath = this.nodePropertiesPage.getContentsGroup().getExistingResourcePath();

			final String domainManagerName = this.nodePropertiesPage.getDomain();
			final SoftPkg[] devices = this.nodeDevicesPage.getNodeDevices();

			BasicNewProjectResourceWizard.updatePerspective(this.fConfig);
			final WorkspaceModifyOperation op = new WorkspaceModifyOperation() {
				@Override
				protected void execute(final IProgressMonitor monitor) throws CoreException {
					try {
						final SubMonitor progress = SubMonitor.convert(monitor, 4);

						// Create an empty project
						final IProject project = NodeProjectCreator.createEmptyProject(projectName, locationURI, progress.newChild(1));
						if (workingSets.length > 0) {
							PlatformUI.getWorkbench().getWorkingSetManager().addToWorkingSets(project, workingSets);
						}

						// If we're creating a new waveform (vs importing one)
						if (isCreateNewResource) {
							// Figure out the ID we'll use 
							String id;
							if (generateId) {
								id = DceUuidUtil.createDceUUID();
							} else {
								id = providedId;
							}

							// Create the SCA XML files
							NewScaNodeProjectWizard.this.openEditorOn = NodeProjectCreator.createNodeFiles(project, id, null, domainManagerName, devices,
							        progress.newChild(1));
						} else {
							NewScaNodeProjectWizard.this.openEditorOn = ProjectCreator.importFile(project, existingDcdPath, progress.newChild(1));
						}

						// Setup automatic RPM spec file generation
						CodegenUtil.addTopLevelRPMSpecBuilder(project, progress.newChild(1));
					} finally {
						monitor.done();
					}
				}
			};
			getContainer().run(false, false, op);
			final IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
			if ((this.openEditorOn != null) && this.openEditorOn.exists()) {
				final NodeEditor nodePart = (NodeEditor) IDE.openEditor(activePage, this.openEditorOn, true);
				setCustomPreferences(nodePart);
			}
		} catch (final InvocationTargetException x) {
			StatusManager.getManager().handle(new Status(IStatus.ERROR, DcdUiActivator.PLUGIN_ID, x.getCause().getMessage(), x.getCause()),
			        StatusManager.SHOW | StatusManager.LOG);
			return false;
		} catch (final InterruptedException x) {
			return false;
		} catch (final PartInitException e) {
			// If the editor cannot be opened, still close the wizard
			return true;
		}
		return true;
	}

	/**
	 * Set custom viewing properties for the Node Editor so that we can tell the difference between this editor and the Node Explorer
	 * 
	 * @param nodePart The Node Editor instance that we shall change the initial diagram style for
	 */
	private void setCustomPreferences(final NodeEditor nodePart) {
		final DiagramGraphicalViewer viewer = (DiagramGraphicalViewer) nodePart.getDiagramGraphicalViewer();
		final IPreferenceStore store = viewer.getWorkspaceViewerPreferenceStore();

		store.setValue(WorkspaceViewerProperties.VIEWRULERS, true);
		store.setValue(WorkspaceViewerProperties.VIEWGRID, true);
		store.setValue(WorkspaceViewerProperties.GRIDSPACING, .5); // SUPPRESS CHECKSTYLE MagicNumber
		store.setValue(WorkspaceViewerProperties.GRIDORDER, false);
		store.setValue(WorkspaceViewerProperties.GRIDLINESTYLE, SWT.LINE_SOLID);
		store.setValue(WorkspaceViewerProperties.GRIDLINECOLOR, SWT.COLOR_BLACK);
	}

	@Override
	public IWizardPage getNextPage(final IWizardPage page) {
		if (!this.nodePropertiesPage.getContentsGroup().isCreateNewResource() && (page == this.nodePropertiesPage)) {
			return null;
		} else {
			return super.getNextPage(page);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	public void init(final IWorkbench arg0, final IStructuredSelection arg1) {

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void addPages() {
		this.nodePropertiesPage = new ScaNodeProjectPropertiesWizardPage("");
		this.nodePropertiesPage.setDescription("Choose to create a new Node or import an existing one.");
		addPage(this.nodePropertiesPage);

		final SdrRoot sdrRoot = SdrUiPlugin.getDefault().getTargetSdrRoot();

		final IRunnableWithProgress waitForLoad = new IRunnableWithProgress() {
			public void run(final IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
				sdrRoot.load(monitor);
			}
		};
		try {
			new ProgressMonitorDialog(getShell()).run(true, false, waitForLoad);
		} catch (final InvocationTargetException e) {
			return;
		} catch (final InterruptedException e) {
			return;
		}

		this.nodeDevicesPage = new ScaNodeProjectDevicesWizardPage("", sdrRoot.getDevicesContainer().getComponents().toArray(new SoftPkg[0]));
		this.nodeDevicesPage.setDescription("Add existing Device(s) to your node.");
		addPage(this.nodeDevicesPage);
	}

	/**
	 * {@inheritDoc}
	 */
	public void setInitializationData(final IConfigurationElement config, final String propertyName, final Object data) throws CoreException {
		this.fConfig = config;
	}

}
