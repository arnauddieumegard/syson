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
package org.eclipse.syson.diagram.common.view.services.description;

import java.util.List;

import org.eclipse.emf.ecore.EClassifier;
import org.eclipse.sirius.components.view.builder.generated.DiagramBuilders;
import org.eclipse.sirius.components.view.builder.generated.ViewBuilders;
import org.eclipse.sirius.components.view.diagram.DiagramToolSection;
import org.eclipse.sirius.components.view.diagram.DropTool;
import org.eclipse.sirius.components.view.diagram.NodeContainmentKind;
import org.eclipse.sirius.components.view.diagram.NodeDescription;
import org.eclipse.sirius.components.view.diagram.NodeTool;
import org.eclipse.sirius.components.view.diagram.NodeToolSection;
import org.eclipse.syson.diagram.common.view.ViewDiagramElementFinder;
import org.eclipse.syson.sysml.SysmlPackage;
import org.eclipse.syson.util.AQLUtils;
import org.eclipse.syson.util.DescriptionNameGenerator;
import org.eclipse.syson.util.IDescriptionNameGenerator;
import org.eclipse.syson.util.SysMLMetamodelHelper;

/**
 * Services class for building View Description.
 *
 * @author adieumegard
 */
public class ToolDescriptionService {

    protected final ViewBuilders viewBuilderHelper = new ViewBuilders();

    protected final DiagramBuilders diagramBuilderHelper = new DiagramBuilders();

    /**
     * Create a {@link DiagramToolSection} containing the {@code Add Existing Elements} tools.
     *
     * @return The created {@link DiagramToolSection}
     */
    public DiagramToolSection addElementsDiagramToolSection() {
        return this.diagramBuilderHelper.newDiagramToolSection()
                .name("Add")
                .nodeTools(this.addExistingElementsTool(false), this.addExistingElementsTool(true))
                .build();
    }

    /**
     * Create a {@link NodeToolSection} containing the {@code Add Existing Elements} tools.
     *
     * @param cache
     *            The {@link ViewDiagramElementFinder} cache
     * @return The created {@link NodeToolSection}
     */
    public NodeToolSection addElementsNodeToolSection() {
        return this.diagramBuilderHelper.newNodeToolSection()
                .name("Add")
                .nodeTools(this.addExistingElementsTool(false), this.addExistingElementsTool(true))
                .build();
    }

    /**
     * Create a {@link NodeTool} adding/displaying existing elements from {@code self} on the context diagram or diagram
     * element.
     *
     * @param recursive
     *            Whether or not the created tool is recursive
     * @return The created {@link NodeTool}
     */
    public NodeTool addExistingElementsTool(boolean recursive) {
        var builder = this.diagramBuilderHelper.newNodeTool();

        var addExistingelements = this.viewBuilderHelper.newChangeContext()
                .expression(AQLUtils.getSelfServiceCallExpression("addExistingElements", List.of("editingContext", "diagramContext", "selectedNode", "convertedNodes", "" + recursive)));

        String title = "Add existing elements";
        String iconURL = "/icons/AddExistingElements.svg";
        if (recursive) {
            title += " (recursive)";
            iconURL = "/icons/AddExistingElementsRecursive.svg";
        }

        return builder
                .name(title)
                .iconURLsExpression(iconURL)
                .body(addExistingelements.build())
                .build();
    }

    public DropTool createDropFromExplorerTool() {
        var dropElementFromExplorer = this.viewBuilderHelper.newChangeContext()
                .expression(AQLUtils.getSelfServiceCallExpression("dropElementFromExplorer", List.of("editingContext", "diagramContext", "selectedNode", "convertedNodes")));

        return this.diagramBuilderHelper.newDropTool()
                .name("Drop from Explorer")
                .body(dropElementFromExplorer.build())
                .build();
    }

    /**
     * Create a {@link NodeTool} to create a new instance of type {@code eClass} represented using the provided
     * {@code nodeDescription}.
     *
     * @param nodeDescription
     *            THe {@link NodeDescription} used to represent the element created using the built {@link NodeTool}
     * @param eClass
     *            The {@link EClassifier} of the created semantic element
     * @param iDescriptionNameGenerator
     *            A {@link DescriptionNameGenerator} to generate the created {@link NodeTool} name
     * @return The created {@link NodeTool}
     */
    public NodeTool createNodeToolFromDiagramBackground(NodeDescription nodeDescription, EClassifier eClass, IDescriptionNameGenerator iDescriptionNameGenerator) {
        var builder = this.diagramBuilderHelper.newNodeTool();

        var changeContextNewInstance = this.viewBuilderHelper.newChangeContext()
                .expression(AQLUtils.getServiceCallExpression("newInstance", "elementInitializer"));

        var createEClassInstance = this.viewBuilderHelper.newCreateInstance()
                .typeName(SysMLMetamodelHelper.buildQualifiedName(eClass))
                .referenceName(SysmlPackage.eINSTANCE.getRelationship_OwnedRelatedElement().getName())
                .variableName("newInstance")
                .children(changeContextNewInstance.build());

        var createView = this.diagramBuilderHelper.newCreateView()
                .containmentKind(NodeContainmentKind.CHILD_NODE)
                .elementDescription(nodeDescription)
                .parentViewExpression("aql:selectedNode")
                .semanticElementExpression("aql:newInstance")
                .variableName("newInstanceView");

        var changeContexMembership = this.viewBuilderHelper.newChangeContext()
                .expression(AQLUtils.getSelfServiceCallExpression("createMembership"))
                .children(createEClassInstance.build(), createView.build());

        return builder
                .name(iDescriptionNameGenerator.getCreationToolName(eClass))
                .iconURLsExpression("/icons/full/obj16/" + eClass.getName() + ".svg")
                .body(changeContexMembership.build())
                .build();
    }

}