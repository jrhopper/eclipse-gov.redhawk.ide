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
package gov.redhawk.eclipsecorba.library.ui;

import gov.redhawk.eclipsecorba.idl.expressions.util.ExpressionsAdapterFactory;
import gov.redhawk.eclipsecorba.idl.operations.provider.OperationsItemProviderAdapterFactory;
import gov.redhawk.eclipsecorba.idl.provider.IdlItemProviderAdapterFactory;
import gov.redhawk.eclipsecorba.idl.types.provider.TypesItemProviderAdapterFactory;
import gov.redhawk.eclipsecorba.library.IdlLibrary;
import gov.redhawk.eclipsecorba.library.provider.RepositoryItemProviderAdapterFactory;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.transaction.TransactionalEditingDomain;
import org.eclipse.emf.transaction.ui.provider.TransactionalAdapterFactoryContentProvider;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.progress.UIJob;

/**
 * A deferred content provided for IdlLibraries.
 * @since 1.1
 */
public class IdlRepositoryContentProvider extends  TransactionalAdapterFactoryContentProvider {
	
	public IdlRepositoryContentProvider(TransactionalEditingDomain editingDomain) {
		this(editingDomain, createAdapterFactory());
	}
	
	public IdlRepositoryContentProvider(TransactionalEditingDomain editingDomain, AdapterFactory adapterFactory) {
		super(editingDomain, adapterFactory);
		Assert.isNotNull(editingDomain);
	}
	
	/**
	 * Creates the adapter factory.
	 * 
	 * @return the adapter factory
	 */
	protected static AdapterFactory createAdapterFactory() {
		final ComposedAdapterFactory adapterFactory = new ComposedAdapterFactory();
		adapterFactory.addAdapterFactory(new IdlItemProviderAdapterFactory());
		adapterFactory.addAdapterFactory(new OperationsItemProviderAdapterFactory());
		adapterFactory.addAdapterFactory(new ExpressionsAdapterFactory());
		adapterFactory.addAdapterFactory(new TypesItemProviderAdapterFactory());
		adapterFactory.addAdapterFactory(new RepositoryItemProviderAdapterFactory());

		return adapterFactory;
	}

	@Override
	public Object getParent(final Object object) {
		if (object instanceof IdlLibrary) {
			return null;
		}
		final Object retVal = super.getParent(object);
		return retVal;
	}
	
	

	@Override
	public Object[] getElements(Object object) {
		final IdlLibrary library = (IdlLibrary) object;
		if (library != null && library.getLoadStatus() == null) {
			final int systemHash = System.identityHashCode(object);
			if (!this.fetched.contains(systemHash)) {
				this.fetched.add(System.identityHashCode(object));
				final FetchJob job = new FetchJob(object, this.viewer) {

					@Override
					protected IStatus doFetch(final IProgressMonitor monitor) {
						try {
							library.load(monitor);
						} catch (CoreException e) {
							return e.getStatus();
						}
						return Status.OK_STATUS;
					}

				};
				job.schedule();

				return new Object[] {
					job
				};
			}
		}
		return super.getChildren(object);
	}
	
	@Override
	public Object[] getChildren(final Object object) {
		return super.getChildren(object);
	}

	@Override
	public boolean hasChildren(final Object object) {
		return super.hasChildren(object);
	}

	private abstract static class FetchJob extends Job {

		private final Object element;
		private final Viewer viewer;

		public FetchJob(final Object element, final Viewer viewer) {
			super("Loading...");
			this.element = element;
			setPriority(Job.LONG);
			this.viewer = viewer;
		}

		protected abstract IStatus doFetch(IProgressMonitor monitor);

		@Override
		protected final IStatus run(final IProgressMonitor monitor) {
			try {
				return doFetch(monitor);
			} finally {
				final UIJob refreshJob = new UIJob("Refresh") {

					@Override
					public IStatus runInUIThread(final IProgressMonitor monitor) {
						if (FetchJob.this.viewer.getControl().isDisposed()) {
							return Status.CANCEL_STATUS;
						}
						if (FetchJob.this.viewer instanceof StructuredViewer) {
							((StructuredViewer) FetchJob.this.viewer).refresh(FetchJob.this.element);
						} else {
							FetchJob.this.viewer.refresh();
						}
						return Status.OK_STATUS;
					}
				};
				refreshJob.setSystem(true);
				refreshJob.schedule();
			}
		}
	}

	private final Set<Integer> fetched = new HashSet<Integer>();

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void dispose() {
		((ComposedAdapterFactory) this.adapterFactory).dispose();
		this.adapterFactory = null;
		this.fetched.clear();
		super.dispose();
	}
	

}
