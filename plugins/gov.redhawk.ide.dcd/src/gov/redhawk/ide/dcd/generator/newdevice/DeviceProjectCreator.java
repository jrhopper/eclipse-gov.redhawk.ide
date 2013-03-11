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
package gov.redhawk.ide.dcd.generator.newdevice;

import gov.redhawk.ide.codegen.util.ProjectCreator;
import gov.redhawk.ide.dcd.IdeDcdPlugin;
import gov.redhawk.ide.natures.ScaComponentProjectNature;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import mil.jpeojtrs.sca.prf.PrfPackage;
import mil.jpeojtrs.sca.scd.ScdPackage;
import mil.jpeojtrs.sca.spd.SpdPackage;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;

public class DeviceProjectCreator extends ProjectCreator {

	private DeviceProjectCreator() {
	}

	/**
	 * Creates a new SCA device project without any files. Should be invoked in the context of a
	 * {@link org.eclipse.ui.actions.WorkspaceModifyOperation WorkspaceModifyOperation}.
	 * 
	 * @param projectName The project name
	 * @param projectLocation the location on disk to create the project
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	 *  to call done() on the given monitor. Accepts null, indicating that no progress should be
	 *  reported and that the operation cannot be canceled.
	 * @return The newly created project
	 * @throws CoreException A problem occurs while creating the project
	 */
	public static IProject createEmptyProject(final String projectName, final URI projectLocation, final IProgressMonitor monitor) throws CoreException {
		SubMonitor progress = SubMonitor.convert(monitor, "Creating emptry project", 2);
		final String[] additionalNatureIDs = new String[] { ScaComponentProjectNature.ID, "org.python.pydev.pythonNature" };
		final IProject project = ProjectCreator.createEmptyProject(projectName, projectLocation, additionalNatureIDs, progress.newChild(1));
		ProjectCreator.resetProject(project, progress.newChild(1));
		return project;
	}

	/**
	 * Creates the basic files for a device in an empty SCA resource project. Should be invoked in the context of a
	 * {@link org.eclipse.ui.actions.WorkspaceModifyOperation WorkspaceModifyOperation}.
	 * 
	 * @param project The project to generate files in
	 * @param projectID The project's ID (DCE)
	 * @param authorName The name of the device author
	 * @param deviceType the type of device
	 * @param aggregateDevice True if an aggregate device
	 * @param monitor the progress monitor to use for reporting progress to the user. It is the caller's responsibility
	 *  to call done() on the given monitor. Accepts null, indicating that no progress should be
	 *  reported and that the operation cannot be canceled.
	 * @return The newly created DCD file
	 * @throws CoreException An error occurs while generating files
	 */
	public static IFile createDeviceFiles(final IProject project, final String projectID, final String authorName, final String deviceType,
	        final boolean aggregateDevice, final IProgressMonitor monitor) throws CoreException {
		final SubMonitor progress = SubMonitor.convert(monitor, "Creating SCA device files", 2);

		final GeneratorArgs args = new GeneratorArgs();
		args.setProjectName(project.getName());
		args.setProjectId(projectID);
		args.setAuthorName(authorName);
		args.setDeviceType(deviceType);
		args.setAggregateDevice(aggregateDevice);
		args.setSoftPkgFile(project.getName() + SpdPackage.FILE_EXTENSION);

		// Generate file content from templates
		final String spd = new SpdFileTemplate().generate(args);
		final String prf = new PrfFileTemplate().generate(args);
		final String scd = new ScdFileTemplate().generate(args);
		final String test = new TestFileTemplate().generate(args);
		progress.worked(1);

		// Check that files/folders don't exist already
		final IFile spdFile = project.getFile(project.getName() + SpdPackage.FILE_EXTENSION);
		if (spdFile.exists()) {
			throw new CoreException(new Status(IStatus.ERROR, IdeDcdPlugin.PLUGIN_ID, "File " + spdFile.getName() + " already exists.", null));
		}

		final IFile prfFile = project.getFile(project.getName() + PrfPackage.FILE_EXTENSION);
		if (prfFile.exists()) {
			throw new CoreException(new Status(IStatus.ERROR, IdeDcdPlugin.PLUGIN_ID, "File " + prfFile.getName() + " already exists.", null));
		}

		final IFile scdFile = project.getFile(project.getName() + ScdPackage.FILE_EXTENSION);
		if (scdFile.exists()) {
			throw new CoreException(new Status(IStatus.ERROR, IdeDcdPlugin.PLUGIN_ID, "File " + scdFile.getName() + " already exists.", null));
		}

		final IFolder testFolder = project.getFolder("tests");
		final IFile testFile = testFolder.getFile("test_" + project.getName() + ".py");
		if (testFolder.exists()) {
			throw new CoreException(new Status(IStatus.ERROR, IdeDcdPlugin.PLUGIN_ID, "Folder " + testFolder.getName() + " already exists.", null));
		}

		// Write files to disk
		try {
			spdFile.create(new ByteArrayInputStream(spd.getBytes("UTF-8")), true, progress.newChild(1));
		} catch (final UnsupportedEncodingException e) {
			throw new CoreException(new Status(IStatus.ERROR, IdeDcdPlugin.PLUGIN_ID, "Internal Error", e));
		}

		try {
			prfFile.create(new ByteArrayInputStream(prf.getBytes("UTF-8")), true, progress.newChild(1));
		} catch (final UnsupportedEncodingException e) {
			throw new CoreException(new Status(IStatus.ERROR, IdeDcdPlugin.PLUGIN_ID, "Internal Error", e));
		}

		try {
			scdFile.create(new ByteArrayInputStream(scd.getBytes("UTF-8")), true, progress.newChild(1));
		} catch (final UnsupportedEncodingException e) {
			throw new CoreException(new Status(IStatus.ERROR, IdeDcdPlugin.PLUGIN_ID, "Internal Error", e));
		}

		testFolder.create(true, true, progress.newChild(1));
		try {
			testFile.create(new ByteArrayInputStream(test.getBytes("UTF-8")), true, progress.newChild(1));
		} catch (final UnsupportedEncodingException e) {
			throw new CoreException(new Status(IStatus.ERROR, IdeDcdPlugin.PLUGIN_ID, "Internal Error", e));
		}

		return spdFile;
	}

}
