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
package gov.redhawk.ide.sdr.ui.internal.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;

import gov.redhawk.ide.sdr.ui.SdrUiPlugin;
import gov.redhawk.ide.sdr.ui.export.FileStoreExporter;
import gov.redhawk.ide.sdr.ui.export.IScaExporter;
import gov.redhawk.ide.sdr.ui.util.ExportToSdrRootJob;
import gov.redhawk.ide.sdr.ui.util.RefreshSdrJob;

public class ExportToSDRHandler extends AbstractHandler implements IHandler {

	@Override
	public Object execute(@Nullable final ExecutionEvent event) throws ExecutionException {
		final ISelection sel = HandlerUtil.getCurrentSelection(event);
		if (sel instanceof IStructuredSelection) {
			List<IProject> projects = new ArrayList<IProject>();
			for (Object obj : ((IStructuredSelection) sel).toList()) {
				if (obj instanceof IProject) {
					projects.add((IProject) obj);
				} else if (obj instanceof IFile) {
					projects.add(((IFile) obj).getProject());
				}
			}

			// Export, then refresh the SDRROOT
			final IScaExporter exporter = new FileStoreExporter(SdrUiPlugin.getDefault().getTargetSdrPath());
			final ExportToSdrRootJob exportJob = new ExportToSdrRootJob(exporter, projects);
			final RefreshSdrJob refreshJob = new RefreshSdrJob();
			exportJob.addJobChangeListener(new JobChangeAdapter() {
				@Override
				public void done(IJobChangeEvent event) {
					refreshJob.schedule();
				}
			});
			exportJob.schedule();
		}

		return null;
	}
}
