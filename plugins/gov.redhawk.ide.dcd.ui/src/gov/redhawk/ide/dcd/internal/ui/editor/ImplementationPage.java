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

package gov.redhawk.ide.dcd.internal.ui.editor;

import gov.redhawk.ide.dcd.internal.ui.HelpContextIds;
import gov.redhawk.ide.dcd.internal.ui.ScaIdeConstants;
import gov.redhawk.ui.editor.ScaFormPage;

import org.eclipse.emf.common.ui.viewer.IViewerProvider;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.IManagedForm;
import org.eclipse.ui.forms.widgets.ScrolledForm;

// TODO: Auto-generated Javadoc
/**
 * The Class ImplementationPage.
 * @since 1.1
 */
public class ImplementationPage extends ScaFormPage implements IViewerProvider {

	/** The Constant PAGE_ID. */
	public static final String PAGE_ID = "implementations"; //$NON-NLS-1$
	private final ImplementationsBlock fBlock;

	/**
	 * Instantiates a new properties form page.
	 * 
	 * @param editor the editor
	 */
	public ImplementationPage(final NodeEditor editor) {
		super(editor, ImplementationPage.PAGE_ID, "Implementations");
		this.fBlock = new ImplementationsBlock(this);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public NodeEditor getEditor() {
		return (NodeEditor) super.getEditor();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected String getHelpResource() {
		return ScaIdeConstants.PLUGIN_DOC_ROOT + "guide/tools/editors/node_editor/implementations.htm"; //$NON-NLS-1$
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void createFormContent(final IManagedForm managedForm) {
		final ScrolledForm form = managedForm.getForm();
		form.setText("Implementations");

		// TODO
		// form.setImage(PDEPlugin.getDefault().getLabelProvider().get(PDEPluginImages.DESC_EXTENSIONS_OBJ));
		this.fBlock.createContent(managedForm);

		// refire selection
		this.fBlock.getSection().fireSelection();
		PlatformUI.getWorkbench().getHelpSystem().setHelp(form.getBody(), HelpContextIds.NODE_OVERVIEW);
		super.createFormContent(managedForm);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Viewer getViewer() {
		return this.fBlock.getSection().getStructuredViewerPart().getViewer();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void refresh(final Resource resource) {
		this.fBlock.refresh(resource);
	}

}
