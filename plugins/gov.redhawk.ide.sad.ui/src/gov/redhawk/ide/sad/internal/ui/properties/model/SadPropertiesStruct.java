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
package gov.redhawk.ide.sad.internal.ui.properties.model;

import java.util.Collection;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.UnexecutableCommand;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.util.FeatureMap;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.domain.EditingDomain;

import mil.jpeojtrs.sca.prf.AbstractPropertyRef;
import mil.jpeojtrs.sca.prf.PrfFactory;
import mil.jpeojtrs.sca.prf.PrfPackage;
import mil.jpeojtrs.sca.prf.Simple;
import mil.jpeojtrs.sca.prf.SimpleSequence;
import mil.jpeojtrs.sca.prf.Struct;
import mil.jpeojtrs.sca.prf.StructRef;

/**
 * 
 */
public class SadPropertiesStruct extends SadPropertyImpl<Struct> implements NestedItemProvider {

	public SadPropertiesStruct(AdapterFactory adapterFactory, Struct def, Object parent) {
		super(adapterFactory, def, parent);
		for (FeatureMap.Entry field : def.getFields()) {
			if (field.getEStructuralFeature() == PrfPackage.Literals.STRUCT__SIMPLE) {
				getChildren().add(new SadPropertiesSimple(adapterFactory, (Simple) field.getValue(), this));
			} else if (field.getEStructuralFeature() == PrfPackage.Literals.STRUCT__SIMPLE_SEQUENCE) {
				getChildren().add(new SadPropertiesSimpleSequence(adapterFactory, (SimpleSequence) field.getValue(), this));
			}
		}
	}

	@Override
	protected StructRef getValueRef() {
		return (StructRef) super.getValueRef();
	}

	@Override
	public Object getSadValue() {
		return null;
	}

	@Override
	public String getPrfValue() {
		return null;
	}

	@Override
	protected Collection< ? > getKindTypes() {
		return getDefinition().getConfigurationKind();
	}

	protected EStructuralFeature getChildFeature(Object object, Object child) {
		switch (((EObject) child).eClass().getClassifierID()) {
		case PrfPackage.SIMPLE_REF:
			return PrfPackage.Literals.STRUCT_REF__SIMPLE_REF;
		case PrfPackage.SIMPLE_SEQUENCE_REF:
			return PrfPackage.Literals.STRUCT_REF__SIMPLE_SEQUENCE_REF;
		}
		return null;
	}

	@Override
	protected Object createModelObject(EStructuralFeature feature, Object value) {
		if (feature == SadPropertiesPackage.Literals.SAD_PROPERTY__VALUE) {
			StructRef ref = PrfFactory.eINSTANCE.createStructRef();
			ref.setRefID(getID());
			ref.getRefs().add(getChildFeature(ref, value), value);
			return ref;
		}
		return super.createModelObject(feature, value);
	}

	@Override
	public Command createAddChildCommand(EditingDomain domain, Object child, EStructuralFeature feature) {
		if (feature == SadPropertiesPackage.Literals.SAD_PROPERTY__VALUE) {
			StructRef ref = getValueRef();
			if (ref == null) {
				return ((NestedItemProvider) getParent()).createAddChildCommand(domain, createModelObject(feature, child), feature);
			} else {
				return AddCommand.create(domain, ref, getChildFeature(ref, child), child);
			}
		}
		return UnexecutableCommand.INSTANCE;
	}

	@Override
	public Command createRemoveChildCommand(EditingDomain domain, Object child, EStructuralFeature feature) {
		if (feature == SadPropertiesPackage.Literals.SAD_PROPERTY__VALUE) {
			StructRef ref = getValueRef();
			if (ref.getRefs().size() == 1) {
				return ((NestedItemProvider)getParent()).createRemoveChildCommand(domain, ref, feature);
			} else {
				return RemoveCommand.create(domain, ref, getChildFeature(ref, child), child);
			}
		}
		return UnexecutableCommand.INSTANCE;
	}

	protected SadPropertyImpl< ? > getField(String identifier) {
		for (Object child : children) {
			SadPropertyImpl< ? > field = (SadPropertyImpl< ? >) child;
			if (field.getID().equals(identifier)) {
				return field;
			}
		}
		return null;
	}

	@Override
	public void referenceAdded(AbstractPropertyRef< ? > reference) {
		super.referenceAdded(reference);
		StructRef structRef = (StructRef) reference;
		for (FeatureMap.Entry ref : structRef.getRefs()) {
			AbstractPropertyRef< ? > childRef = (AbstractPropertyRef< ? >)ref.getValue();
			SadPropertyImpl< ? > field = getField(childRef.getRefID());
			field.referenceAdded(childRef);
		}
	}

	@Override
	public void referenceRemoved(AbstractPropertyRef< ? > reference) {
		super.referenceRemoved(reference);
		StructRef structRef = (StructRef) reference;
		for (FeatureMap.Entry ref : structRef.getRefs()) {
			AbstractPropertyRef< ? > childRef = (AbstractPropertyRef< ? >)ref.getValue();
			SadPropertyImpl< ? > field = getField(childRef.getRefID());
			field.referenceRemoved(childRef);
		}
	}
}
