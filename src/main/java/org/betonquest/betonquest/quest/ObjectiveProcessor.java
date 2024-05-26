package org.betonquest.betonquest.quest;

import org.betonquest.betonquest.Instruction;
import org.betonquest.betonquest.api.Objective;
import org.betonquest.betonquest.api.config.quest.QuestPackage;
import org.betonquest.betonquest.api.logger.BetonQuestLogger;
import org.betonquest.betonquest.api.profiles.Profile;
import org.betonquest.betonquest.bstats.CompositeInstructionMetricsSupplier;
import org.betonquest.betonquest.exceptions.InstructionParseException;
import org.betonquest.betonquest.exceptions.ObjectNotFoundException;
import org.betonquest.betonquest.id.ObjectiveID;
import org.bukkit.configuration.ConfigurationSection;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Stores Objectives and starts/stops/resumes them.
 */
public class ObjectiveProcessor extends QuestProcessor<Objective, ObjectiveID> {
    /**
     * Available Objective types
     */
    private final Map<String, Class<? extends Objective>> objectiveTypes;

    /**
     * Create a new Objective Processor to store Objectives and starts/stops/resumes them.
     *
     * @param log            the custom logger for this class
     * @param objectiveTypes the available objective types
     */
    public ObjectiveProcessor(final BetonQuestLogger log, final Map<String, Class<? extends Objective>> objectiveTypes) {
        super(log);
        this.objectiveTypes = objectiveTypes;
    }

    @Override
        /* default */ void clear() {
        // save data of all objectives to the players
        for (final Objective objective : values.values()) {
            objective.close();
        }
        super.clear();
    }

    /* default */ CompositeInstructionMetricsSupplier<ObjectiveID> metricsSupplier() {
        return new CompositeInstructionMetricsSupplier<>(values::keySet, objectiveTypes::keySet);
    }

    @SuppressWarnings({"PMD.CognitiveComplexity", "PMD.CyclomaticComplexity"})
    @Override
    public void load(final QuestPackage pack) {
        final ConfigurationSection oConfig = pack.getConfig().getConfigurationSection("objectives");
        if (oConfig != null) {
            final String packName = pack.getQuestPath();
            for (final String key : oConfig.getKeys(false)) {
                if (key.contains(" ")) {
                    log.warn(pack, "Objective name cannot contain spaces: '" + key + "' (in " + packName + " package)");
                    continue;
                }
                final ObjectiveID identifier;
                try {
                    identifier = new ObjectiveID(pack, key);
                } catch (final ObjectNotFoundException e) {
                    log.warn(pack, "Error while loading objective '" + packName + "." + key + "': " + e.getMessage(), e);
                    continue;
                }
                final String type;
                try {
                    type = identifier.generateInstruction().getPart(0);
                } catch (final InstructionParseException e) {
                    log.warn(pack, "Objective type not defined in '" + packName + "." + key + "'", e);
                    continue;
                }
                final Class<? extends Objective> objectiveClass = objectiveTypes.get(type);
                // if it's null then there is no such type registered, log an error
                if (objectiveClass == null) {
                    log.warn(pack,
                            "Objective type " + type + " is not registered, check if it's"
                                    + " spelled correctly in '" + identifier + "' objective.");
                    continue;
                }
                try {
                    final Objective objective = objectiveClass.getConstructor(Instruction.class)
                            .newInstance(identifier.generateInstruction());
                    values.put(identifier, objective);
                    log.debug(pack, "  Objective '" + identifier + "' loaded");
                } catch (final InvocationTargetException e) {
                    if (e.getCause() instanceof InstructionParseException) {
                        log.warn(pack, "Error in '" + identifier + "' objective (" + type + "): " + e.getCause().getMessage(), e);
                    } else {
                        log.reportException(pack, e);
                    }
                } catch (final NoSuchMethodException | InstantiationException | IllegalAccessException e) {
                    log.reportException(pack, e);
                }
            }
        }
    }

    /**
     * Creates new objective for given player
     *
     * @param profile     the {@link Profile} of the player
     * @param objectiveID ID of the objective
     */
    public void newObjective(final Profile profile, final ObjectiveID objectiveID) {
        final Objective objective = values.get(objectiveID);
        if (objective.containsPlayer(profile)) {
            log.debug(objectiveID.getPackage(), profile + " already has the " + objectiveID + " objective");
            return;
        }
        objective.newPlayer(profile);
    }

    /**
     * Resumes the existing objective for given player
     *
     * @param profile     the {@link Profile} of the player
     * @param objectiveID ID of the objective
     * @param instruction data instruction string
     */
    public void resumeObjective(final Profile profile, final ObjectiveID objectiveID, final String instruction) {
        final Objective objective = values.get(objectiveID);
        if (objective == null) {
            log.warn(objectiveID.getPackage(), "Objective " + objectiveID + " does not exist");
            return;
        }
        if (objective.containsPlayer(profile)) {
            log.debug(objectiveID.getPackage(), profile + " already has the " + objectiveID + " objective!");
            return;
        }
        objective.resumeObjectiveForPlayer(profile, instruction);
    }

    /**
     * @param objectiveID package name, dot and ID of the objective
     * @return Objective object or null if it does not exist
     */
    @Nullable
    public Objective getObjective(final ObjectiveID objectiveID) {
        return values.get(objectiveID);
    }

    /**
     * Returns the list of objectives of this player
     *
     * @param profile the {@link Profile} of the player
     * @return list of this player's active objectives
     */
    public List<Objective> getPlayerObjectives(final Profile profile) {
        final List<Objective> list = new ArrayList<>();
        for (final Objective objective : values.values()) {
            if (objective.containsPlayer(profile)) {
                list.add(objective);
            }
        }
        return list;
    }

    /**
     * Renames the objective instance.
     *
     * @param name   the current name
     * @param rename the name it should have now
     */
    public void renameObjective(final ObjectiveID name, final ObjectiveID rename) {
        final Objective objective = values.remove(name);
        values.put(rename, objective);
        if (objective != null) {
            objective.setLabel(rename);
        }
    }
}
