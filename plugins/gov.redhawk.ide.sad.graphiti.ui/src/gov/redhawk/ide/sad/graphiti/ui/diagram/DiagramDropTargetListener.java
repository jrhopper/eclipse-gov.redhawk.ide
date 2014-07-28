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
package gov.redhawk.ide.sad.graphiti.ui.diagram;

import gov.redhawk.ide.sad.graphiti.ui.diagram.features.create.ComponentCreateFeature;
import mil.jpeojtrs.sca.spd.SoftPkg;

import org.eclipse.gef.EditPartViewer;
import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.dnd.AbstractTransferDropTargetListener;
import org.eclipse.gef.requests.CreateRequest;
import org.eclipse.gef.requests.CreationFactory;
import org.eclipse.graphiti.features.ICreateFeature;
import org.eclipse.graphiti.ui.editor.DiagramBehavior;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.DND;

public class DiagramDropTargetListener extends AbstractTransferDropTargetListener {

	private DiagramBehavior diagramBehavior;

	public DiagramDropTargetListener(EditPartViewer viewer, DiagramBehavior diagramBehavior) {
		super(viewer, LocalSelectionTransfer.getTransfer());
		setEnablementDeterminedByCommand(true);
		this.diagramBehavior = diagramBehavior;
	}
	
	@Override
	protected void handleDrop() {
		super.handleDrop();
		if (getCurrentEvent().detail == DND.DROP_MOVE) {
			getCurrentEvent().detail = DND.DROP_COPY;
		}
		// Bug 378083 - set focus to diagram editor after drop
		getViewer().getControl().setFocus();
	}

	@Override
	protected void updateTargetRequest() {
		((CreateRequest) getTargetRequest()).setLocation(getDropLocation());
	}
	
	@Override
	protected Request createTargetRequest() {
		CreateRequest request = new CreateRequest();

		request.setFactory(new MyCreationFactory());
		request.setLocation(getDropLocation());
		return request;
	}
	
	private class MyCreationFactory implements CreationFactory {

		public MyCreationFactory() {
		}

		public Object getNewObject() {
			ISelection selection = LocalSelectionTransfer.getTransfer().getSelection();
			
			if (((IStructuredSelection) selection).getFirstElement() instanceof SoftPkg) {
				SoftPkg spd = (SoftPkg) ((IStructuredSelection) selection).getFirstElement();
				ICreateFeature newFeature = new ComponentCreateFeature(diagramBehavior.getDiagramTypeProvider().getFeatureProvider(), spd);
				
				return newFeature;
			}
			
			return LocalSelectionTransfer.getTransfer().getSelection();
		}

		public Object getObjectType() {
			return ICreateFeature.class;
		}
	}

	@Override
	protected void handleDragOver() {

		super.handleDragOver();

		Command command = getCommand();
		if (command != null && command.canExecute()) {
			getCurrentEvent().detail = DND.DROP_COPY;
		}
	}

}