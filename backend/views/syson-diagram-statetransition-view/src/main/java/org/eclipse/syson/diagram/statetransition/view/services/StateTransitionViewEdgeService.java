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
package org.eclipse.syson.diagram.statetransition.view.services;

import java.util.Objects;

import org.eclipse.sirius.components.core.api.IFeedbackMessageService;
import org.eclipse.sirius.components.representations.Message;
import org.eclipse.sirius.components.representations.MessageLevel;
import org.eclipse.syson.diagram.statetransition.view.StateTransitionViewDiagramDescriptionProvider;
import org.eclipse.syson.sysml.Element;
import org.eclipse.syson.sysml.StateDefinition;
import org.eclipse.syson.sysml.StateUsage;

/**
 * Edge-related Java services used by the {@link StateTransitionViewDiagramDescriptionProvider}.
 *
 * @author adieumegard
 */
public class StateTransitionViewEdgeService {

    private final IFeedbackMessageService feedbackMessageService;

    public StateTransitionViewEdgeService(IFeedbackMessageService feedbackMessageService) {
        this.feedbackMessageService = Objects.requireNonNull(feedbackMessageService);
    }

    public boolean checkFeatureTypingEdgeReconnectionTarget(Element usage, Element newTarget) {
        boolean validTarget = false;
        // TODO: update
        if (usage instanceof StateUsage) {
            validTarget = newTarget instanceof StateDefinition;
        }
//        else if (usage instanceof PortUsage) {
//            validTarget = newTarget instanceof PortDefinition;
//        } else if (usage instanceof InterfaceUsage) {
//            validTarget = newTarget instanceof InterfaceDefinition;
//        } else if (usage instanceof PartUsage) {
//            validTarget = newTarget instanceof PartDefinition;
//        } else if (usage instanceof ItemUsage) {
//            validTarget = newTarget instanceof ItemDefinition;
//        }
        if (!validTarget) {
            this.feedbackMessageService.addFeedbackMessage(new Message("The type of a " + usage.eClass().getName() + " cannot be a " + newTarget.eClass().getName(), MessageLevel.WARNING));
        }
        return validTarget;
    }
}
