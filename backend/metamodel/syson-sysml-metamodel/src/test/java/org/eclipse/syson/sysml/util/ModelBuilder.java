/*******************************************************************************
 * Copyright (c) 2024 Obeo.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package org.eclipse.syson.sysml.util;

import static java.util.stream.Collectors.toMap;

import java.util.Map;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.util.ECrossReferenceAdapter;
import org.eclipse.syson.sysml.Classifier;
import org.eclipse.syson.sysml.Definition;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.EndFeatureMembership;
import org.eclipse.syson.sysml.EnumerationDefinition;
import org.eclipse.syson.sysml.EnumerationUsage;
import org.eclipse.syson.sysml.Feature;
import org.eclipse.syson.sysml.FeatureChaining;
import org.eclipse.syson.sysml.FeatureMembership;
import org.eclipse.syson.sysml.FeatureTyping;
import org.eclipse.syson.sysml.OwningMembership;
import org.eclipse.syson.sysml.Redefinition;
import org.eclipse.syson.sysml.ReferenceSubsetting;
import org.eclipse.syson.sysml.ReferenceUsage;
import org.eclipse.syson.sysml.Relationship;
import org.eclipse.syson.sysml.Subclassification;
import org.eclipse.syson.sysml.Subsetting;
import org.eclipse.syson.sysml.Succession;
import org.eclipse.syson.sysml.SysmlFactory;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.sysml.Type;
import org.eclipse.syson.sysml.Usage;
import org.eclipse.syson.sysml.VariantMembership;
import org.eclipse.syson.sysml.VisibilityKind;
import org.eclipse.syson.sysml.helper.EMFUtils;

/**
 * Helper used to build SysML model.
 *
 * @author Arthur Daussy
 */
public class ModelBuilder {

    private static final Map<Class<?>, EClass> CLASS_TO_ECLASS;

    private final SysmlFactory fact = SysmlFactory.eINSTANCE;

    static {
        CLASS_TO_ECLASS = EMFUtils.eAllContentSteamWithSelf(SysmlPackage.eINSTANCE).filter(e -> e instanceof EClass eClass && !eClass.isAbstract() && !eClass.isInterface()).map(EClass.class::cast)
                .collect(toMap(EClass::getInstanceClass, e -> e));
    }

    private final ECrossReferenceAdapter crossReferencerAdapter = new ECrossReferenceAdapter();

    public void addSuperType(Classifier child, Classifier parent) {
        Subclassification subClassification = this.create(Subclassification.class);
        // Workaround waiting for the subset/refine implementation
        subClassification.setGeneral(parent);
        subClassification.setSpecific(child);
        subClassification.setSuperclassifier(parent);
        subClassification.setSubclassifier(child);

        child.getOwnedRelationship().add(subClassification);

    }

    public void addSubsetting(Feature child, Feature parent) {
        Subsetting subsetting = this.create(Subsetting.class);
        // Workaround waiting for the subset/refine implementation
        subsetting.setGeneral(parent);
        subsetting.setSpecific(child);
        subsetting.setSubsettingFeature(child);
        subsetting.setSubsettedFeature(parent);

        child.getOwnedRelationship().add(subsetting);

    }

    public ReferenceSubsetting addReferenceSubsetting(Usage child, Feature parent) {
        ReferenceSubsetting referenceSubsetting = this.create(ReferenceSubsetting.class);
        // Workaround waiting for the subset/refine implementation
        referenceSubsetting.setGeneral(parent);
        referenceSubsetting.setSpecific(child);
        referenceSubsetting.setSubsettingFeature(child);
        referenceSubsetting.setSubsettedFeature(parent);
        referenceSubsetting.setReferencedFeature(parent);

        child.getOwnedRelationship().add(referenceSubsetting);

        return referenceSubsetting;

    }

    public void addRedefinition(Feature redefiningFeature, Feature redefinedFeature) {
        Redefinition redefinition = this.create(Redefinition.class);
        redefinition.setGeneral(redefiningFeature);
        redefinition.setSpecific(redefinedFeature);
        // Workaround waiting for the subset/refine implementation
        redefinition.setRedefinedFeature(redefinedFeature);
        redefinition.setRedefiningFeature(redefiningFeature);
        redefinition.setSubsettedFeature(redefinedFeature);
        redefinition.setSubsettingFeature(redefiningFeature);

        redefiningFeature.getOwnedRelationship().add(redefinition);

    }

    public <T extends Element> T create(Class<T> type) {
        return this.createIn(type, null);
    }

    public <T extends Element> T createWithName(Class<T> type, String name) {
        return this.createInWithFullName(type, null, name, null);
    }

    public <T extends Element> T createIn(Class<T> type, Element parent) {
        return this.createInWithName(type, parent, null);
    }

    public <T extends Element> T createInWithName(Class<T> type, Element parent, String name) {
        return this.createInWithFullName(type, parent, name, null);
    }

    public Feature createFeatureChaining(Feature... featuresToChain) {
        Feature feature = this.create(Feature.class);
        for (Feature f : featuresToChain) {
            FeatureChaining chain = this.create(FeatureChaining.class);
            chain.setChainingFeature(f);
            feature.getOwnedRelationship().add(chain);
        }

        return feature;
    }

    public <T extends Succession> T createSuccessionAsUsage(Class<T> type, Element parent, Feature source, Feature target) {
        T succession = this.create(type);

        // Set source
        ReferenceUsage sourceRefUsage = this.create(ReferenceUsage.class);
        sourceRefUsage.setIsEnd(true);
        ReferenceSubsetting sourceReferenceSubsetting = this.addReferenceSubsetting(sourceRefUsage, source);
        // FeatureChains are directly contained by the Subsetting relationship
        if (!source.getOwnedFeatureChaining().isEmpty()) {
            sourceReferenceSubsetting.getOwnedRelatedElement().add(source);
        }

        EndFeatureMembership sourceEndFeatureMembership = this.create(EndFeatureMembership.class);
        succession.getOwnedRelationship().add(sourceEndFeatureMembership);
        sourceEndFeatureMembership.getOwnedRelatedElement().add(sourceRefUsage);

        // Set target
        ReferenceUsage targetTefUsage = this.create(ReferenceUsage.class);
        targetTefUsage.setIsEnd(true);
        ReferenceSubsetting targetReferenceSubsetting = this.addReferenceSubsetting(targetTefUsage, target);
        
        // FeatureChains are directly contained by the Subsetting relationship
        if (!target.getOwnedFeatureChaining().isEmpty()) {
            targetReferenceSubsetting.getOwnedRelatedElement().add(target);
        }

        EndFeatureMembership targetEndFeatureMembership = this.create(EndFeatureMembership.class);
        succession.getOwnedRelationship().add(targetEndFeatureMembership);
        targetEndFeatureMembership.getOwnedRelatedElement().add(targetTefUsage);

        // this.addFeatureMembership(succession, targetTefUsage);
        this.addFeatureMembership(parent, succession);

        return succession;
    }

    public void setType(Feature feature, Type type) {
        FeatureTyping featureTyping = this.fact.createFeatureTyping();
        featureTyping.setSpecific(feature);
        featureTyping.setTypedFeature(feature);
        // Waiting for subset/refine implementation
        featureTyping.setType(type);
        featureTyping.setGeneral(type);
        feature.getOwnedRelationship().add(featureTyping);
    }

    public <T extends Element> T createInWithFullName(Class<T> type, Element parent, String name, String shortName) {
        T newInstance = (T) this.fact.create(CLASS_TO_ECLASS.get(type));
        newInstance.eAdapters().add(this.crossReferencerAdapter);
        if (name != null) {
            newInstance.setDeclaredName(name);
        }

        if (shortName != null) {
            newInstance.setDeclaredShortName(shortName);
        }
        if (parent != null) {
            if (newInstance instanceof Relationship newInstanceRelationship && !(newInstance instanceof Definition)) {
                parent.getOwnedRelationship().add(newInstanceRelationship);
            } else if (newInstance instanceof EnumerationUsage feature && parent instanceof EnumerationDefinition) {
                this.addVariantMembership(parent, feature);
            } else if (newInstance instanceof Feature feature) {
                this.addFeatureMembership(parent, feature);
            } else {
                this.addOwnedMembership(parent, newInstance);
            }
        }
        return newInstance;
    }

    private void addVariantMembership(Element parent, EnumerationUsage feature) {
        this.addOwnedMembership(parent, feature, VariantMembership.class, null);
    }

    private void addOwnedMembership(Element parent, Element ownedElement) {
        this.addOwnedMembership(parent, ownedElement, OwningMembership.class, null);
    }

    private <T extends OwningMembership> T addOwnedMembership(Element parent, Element ownedElement, Class<T> relationshipType, VisibilityKind visibility) {
        T ownedRelation = (T) this.fact.create(CLASS_TO_ECLASS.get(relationshipType));
        parent.getOwnedRelationship().add(ownedRelation);
        ownedRelation.getOwnedRelatedElement().add(ownedElement);
        // I guess this should be done automatically when adding to ownedRelatedElement but waiting for subset and super
        // set to be implemented
        ownedRelation.getTarget().add(ownedElement);
        ownedRelation.setMemberElement(ownedElement);

        if (visibility != null) {

            ownedRelation.setVisibility(visibility);
        }

        return ownedRelation;

    }

    private void addFeatureMembership(Element parent, Feature feature) {
        FeatureMembership relationship = this.addOwnedMembership(parent, feature, FeatureMembership.class, null);
        // Workaround waiting for subset/refine implementation
        relationship.setFeature(feature);
    }

}
