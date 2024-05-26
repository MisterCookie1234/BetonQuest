package org.betonquest.betonquest.quest;

import org.betonquest.betonquest.api.Condition;
import org.betonquest.betonquest.api.Objective;
import org.betonquest.betonquest.api.Variable;
import org.betonquest.betonquest.api.logger.BetonQuestLogger;
import org.betonquest.betonquest.bstats.CompositeInstructionMetricsSupplier;
import org.betonquest.betonquest.bstats.InstructionMetricsSupplier;
import org.betonquest.betonquest.config.Config;
import org.betonquest.betonquest.config.QuestCanceler;
import org.betonquest.betonquest.conversation.ConversationData;
import org.betonquest.betonquest.id.ConversationID;
import org.betonquest.betonquest.id.ID;
import org.betonquest.betonquest.id.QuestCancelerID;
import org.betonquest.betonquest.id.VariableID;
import org.betonquest.betonquest.quest.event.legacy.QuestEventFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Stores the active Quest Types, Conversations and Quest Canceller.
 */
public class QuestRegistry {
    /**
     * Loaded Variables.
     */
    public final Map<VariableID, Variable> variables = new HashMap<>();

    /**
     * Loaded Conversations.
     */
    public final Map<ConversationID, ConversationData> conversations = new HashMap<>();

    /**
     * Loaded Quest Canceller.
     */
    public final Map<QuestCancelerID, QuestCanceler> cancelers = new HashMap<>();

    /**
     * Available Variable types.
     */
    private final Map<String, Class<? extends Variable>> variableTypes;

    /**
     * Condition logic.
     */
    private final ConditionProcessor conditionProcessor;

    /**
     * Event logic.
     */
    private final EventProcessor eventProcessor;

    /**
     * Objective logic.
     */
    private final ObjectiveProcessor objectiveProcessor;

    /**
     * Create a new Registry for storing and using Conditions, Events, Objectives, Variables,
     * Conversations and Quest canceller.
     *
     * @param log            the custom logger for this registry and processors
     * @param conditionTypes the available condition types
     * @param eventTypes     the available event types
     * @param objectiveTypes the available objective types
     * @param variableTypes  the available variable types
     */
    public QuestRegistry(final BetonQuestLogger log,
                         final Map<String, Class<? extends Condition>> conditionTypes, final Map<String, QuestEventFactory> eventTypes,
                         final Map<String, Class<? extends Objective>> objectiveTypes, final Map<String, Class<? extends Variable>> variableTypes) {
        this.variableTypes = variableTypes;
        this.conditionProcessor = new ConditionProcessor(log, conditionTypes);
        this.eventProcessor = new EventProcessor(log, eventTypes);
        this.objectiveProcessor = new ObjectiveProcessor(log, objectiveTypes);
    }

    /**
     * Clears the loaded data.
     */
    public void clear() {
        conditionProcessor.clear();
        eventProcessor.clear();
        objectiveProcessor.clear();
        conversations.clear();
        variables.clear();
        cancelers.clear();
    }

    public void printSize(final BetonQuestLogger log) {
        log.info("There are " + conditionProcessor.size() + " conditions, " + eventProcessor.size() + " events, "
                + objectiveProcessor.size() + " objectives and " + conversations.size() + " conversations loaded from "
                + Config.getPackages().size() + " packages.");
    }

    /**
     * Gets the bstats metric supplier for registered and active quest types.
     *
     * @return instruction metrics for conditions, events, objectives and variables
     */
    public Map<String, InstructionMetricsSupplier<? extends ID>> metricsSupplier() {
        final Map<String, InstructionMetricsSupplier<? extends ID>> metricsSuppliers = new HashMap<>();
        metricsSuppliers.put("conditions", conditionProcessor.metricsSupplier());
        metricsSuppliers.put("events", eventProcessor.metricsSupplier());
        metricsSuppliers.put("objectives", objectiveProcessor.metricsSupplier());
        metricsSuppliers.put("variables", new CompositeInstructionMetricsSupplier<>(variables::keySet, variableTypes::keySet));
        return metricsSuppliers;
    }

    /**
     * Gets the class processing condition logic.
     *
     * @return condition logic
     */
    public ConditionProcessor conditions() {
        return conditionProcessor;
    }

    /**
     * Gets the class processing event logic.
     *
     * @return event logic
     */
    public EventProcessor events() {
        return eventProcessor;
    }

    /**
     * Gets the class processing objective logic.
     *
     * @return objective logic
     */
    public ObjectiveProcessor objectives() {
        return objectiveProcessor;
    }
}
