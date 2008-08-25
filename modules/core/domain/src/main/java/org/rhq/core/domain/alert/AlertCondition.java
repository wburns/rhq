/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.core.domain.alert;

import java.io.Serializable;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.rhq.core.domain.measurement.MeasurementDefinition;

/**
 * An alert condition (e.g. ActiveThreads > 100) as configured in a JON alert definition.
 */
@Entity
@NamedQueries( {
    @NamedQuery(name = "AlertCondition.findByTriggerId", query = "SELECT a FROM AlertCondition AS a WHERE a.triggerId = :tid"),
    @NamedQuery(name = "AlertCondition.findAll", query = "SELECT a FROM AlertCondition AS a"),
    @NamedQuery(name = AlertCondition.QUERY_DELETE_BY_RESOURCES, query = "DELETE FROM AlertCondition ac WHERE ac.alertDefinition IN ( SELECT ad FROM AlertDefinition ad WHERE ad.resource IN (:resources))"),
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_BASELINE, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionBaselineCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         ms.id, " //
        + "         mb.id, " //
        + "         mb.baselineMean " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ac.measurementDefinition md, MeasurementSchedule ms JOIN ms.baseline mb " //
        + "   WHERE ad.recoveryId = 0 " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ms.definition = md " //
        + "     AND ms.resource = ad.resource " //
        + "     AND mb IS NOT NULL " //
        + "     AND ac.category = 'BASELINE' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_CHANGE, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionChangesCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         ms.id " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ac.measurementDefinition md, MeasurementSchedule ms " //
        + "   WHERE ad.recoveryId = 0 " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ms.definition = md " //
        + "     AND ms.resource = ad.resource " //
        + "     AND ac.category = 'CHANGE' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_TRAIT, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionTraitCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         ms.id, " //
        + "         (" //
        + "           SELECT md.value " //
        + "             FROM MeasurementDataTrait md " //
        + "            WHERE md.schedule = ms " //
        + "              AND md.id.timestamp = " //
        + "                ( " //
        + "                  SELECT max(imd.id.timestamp) " //
        + "                    FROM MeasurementDataTrait imd " //
        + "                   WHERE ms.id = imd.schedule.id " //
        + "                ) " //
        + "         ) " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ac.measurementDefinition md, MeasurementSchedule ms " //
        + "   WHERE ad.recoveryId = 0 " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ms.definition = md " //
        + "     AND ms.resource = ad.resource " //
        + "     AND ac.category = 'TRAIT' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_AVAILABILITY, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionAvailabilityCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         res.id, " //
        + "         (" //
        + "           SELECT a.availabilityType " //
        + "             FROM Availability a " //
        + "             JOIN a.resource ar " //
        + "            WHERE ar = res " //
        + "              AND a.endTime IS NULL " //
        + "         ) " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "   WHERE ad.recoveryId = 0 " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ac.category = 'AVAILABILITY' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_CONTROL, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionControlCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         res.id, " //
        + "         (" //
        + "           SELECT op.id " //
        + "             FROM OperationDefinition op " //
        + "            WHERE op.resourceType = type " //
        + "              AND op.name = ac.name " //
        + "         ) " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "    JOIN res.resourceType type " //
        + "   WHERE ad.recoveryId = 0 " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ac.category = 'CONTROL' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_THRESHOLD, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionScheduleCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         ms.id " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ac.measurementDefinition md, MeasurementSchedule ms " //
        + "   WHERE ad.recoveryId = 0 " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ms.definition = md " //
        + "     AND ms.resource = ad.resource " //
        + "     AND ac.category = 'THRESHOLD' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_EVENT, query = "" //
        + "  SELECT new org.rhq.core.domain.alert.composite.AlertConditionEventCategoryComposite " //
        + "       ( " //
        + "         ac, " //
        + "         res.id " //
        + "       ) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "    JOIN ad.resource res " //
        + "   WHERE ad.recoveryId = 0 " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ac.category = 'EVENT' " //
        + "ORDER BY ac.id"), //
    @NamedQuery(name = AlertCondition.QUERY_BY_CATEGORY_COUNT_PARAMETERIZED, query = "" //
        + "  SELECT count(ac.id) " //
        + "    FROM AlertCondition AS ac " //
        + "    JOIN ac.alertDefinition ad " //
        + "   WHERE ad.recoveryId = 0 " //
        + "     AND ad.enabled = TRUE " //
        + "     AND ad.deleted = FALSE " //
        + "     AND ac.category = :category " //
        + "     AND ad.resource IS NOT NULL") })
@SequenceGenerator(name = "RHQ_ALERT_CONDITION_ID_SEQ", sequenceName = "RHQ_ALERT_CONDITION_ID_SEQ")
@Table(name = "RHQ_ALERT_CONDITION")
public class AlertCondition implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String QUERY_DELETE_BY_RESOURCES = "AlertCondition.deleteByResources";
    public static final String QUERY_BY_CATEGORY_BASELINE = "AlertCondition.byCategoryBaseline";
    public static final String QUERY_BY_CATEGORY_CHANGE = "AlertCondition.byCategoryChange";
    public static final String QUERY_BY_CATEGORY_TRAIT = "AlertCondition.byCategoryTrait";
    public static final String QUERY_BY_CATEGORY_AVAILABILITY = "AlertCondition.byCategoryAvailability";
    public static final String QUERY_BY_CATEGORY_CONTROL = "AlertCondition.byCategoryControl";
    public static final String QUERY_BY_CATEGORY_THRESHOLD = "AlertCondition.byCategoryThreshold";
    public static final String QUERY_BY_CATEGORY_EVENT = "AlertCondition.byCategoryEvent";
    public static final String QUERY_BY_CATEGORY_COUNT_PARAMETERIZED = "AlertCondition.byCategoryCount";

    @Column(name = "ID", nullable = false)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "RHQ_ALERT_CONDITION_ID_SEQ")
    @Id
    @SuppressWarnings( { "unused" })
    private int id;

    @Column(name = "TYPE", nullable = false)
    @Enumerated(EnumType.STRING)
    private AlertConditionCategory category;

    @JoinColumn(name = "MEASUREMENT_DEFINITION_ID", referencedColumnName = "ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private MeasurementDefinition measurementDefinition;

    @Column(name = "NAME")
    private String name;

    @Column(name = "COMPARATOR")
    private String comparator;

    @Column(name = "THRESHOLD")
    private Double threshold;

    @Column(name = "OPTION_STATUS")
    private String option;

    @Column(name = "TRIGGER_ID")
    private Integer triggerId;

    @JoinColumn(name = "ALERT_DEFINITION_ID", referencedColumnName = "ID")
    @ManyToOne(fetch = FetchType.LAZY)
    private AlertDefinition alertDefinition;

    @OneToMany(mappedBy = "condition", cascade = CascadeType.ALL)
    @OrderBy
    // primary key
    private Set<AlertConditionLog> conditionLogs = new LinkedHashSet<AlertConditionLog>();

    /**
     * Creates a new alert condition.
     */
    public AlertCondition() {
    }

    public AlertCondition(AlertDefinition alertDef, AlertConditionCategory type) {
        this.alertDefinition = alertDef;
        this.category = type;
    }

    /**
     * Creates a skeletal copy of the specified alert condition.
     *
     * @param cond the alert condition to be copied
     */
    public AlertCondition(AlertCondition cond) {
        // Don't copy the id.
        this.category = cond.category;
        this.measurementDefinition = cond.measurementDefinition;
        this.name = cond.name;
        this.comparator = cond.comparator;
        this.threshold = cond.threshold;
        this.option = cond.option;
        this.triggerId = cond.triggerId;
        // Don't copy the condition logs.
    }

    public int getId() {
        return this.id;
    }

    public AlertConditionCategory getCategory() {
        return this.category;
    }

    public void setCategory(AlertConditionCategory category) {
        this.category = category;
    }

    public MeasurementDefinition getMeasurementDefinition() {
        return this.measurementDefinition;
    }

    public void setMeasurementDefinition(MeasurementDefinition measurementDefinition) {
        this.measurementDefinition = measurementDefinition;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComparator() {
        return this.comparator;
    }

    public void setComparator(String comparator) {
        this.comparator = comparator;
    }

    public Double getThreshold() {
        return this.threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public String getOption() {
        return this.option;
    }

    public void setOption(String option) {
        this.option = option;
    }

    public Integer getTriggerId() {
        return this.triggerId;
    }

    public void setTriggerId(Integer triggerId) {
        this.triggerId = triggerId;
    }

    public AlertDefinition getAlertDefinition() {
        return this.alertDefinition;
    }

    public void setAlertDefinition(AlertDefinition alertDef) {
        this.alertDefinition = alertDef;
    }

    public Set<AlertConditionLog> getConditionLogs() {
        return this.conditionLogs;
    }

    public void addConditionLog(AlertConditionLog condLog) {
        this.conditionLogs.add(condLog);
        condLog.setCondition(this);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null) {
            return false;
        }

        if (!(obj instanceof AlertCondition)) {
            return false;
        }

        final AlertCondition other = (AlertCondition) obj;
        if (category == null) {
            if (other.category != null) {
                return false;
            }
        } else if (!category.equals(other.category)) {
            return false;
        }

        if (comparator == null) {
            if (other.comparator != null) {
                return false;
            }
        } else if (!comparator.equals(other.comparator)) {
            return false;
        }

        if (id != other.id) {
            return false;
        }

        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }

        if (option == null) {
            if (other.option != null) {
                return false;
            }
        } else if (!option.equals(other.option)) {
            return false;
        }

        if (threshold == null) {
            if (other.threshold != null) {
                return false;
            }
        } else if (!threshold.equals(other.threshold)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((category == null) ? 0 : category.hashCode());
        result = (prime * result) + ((comparator == null) ? 0 : comparator.hashCode());
        result = (prime * result) + id;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        result = (prime * result) + ((option == null) ? 0 : option.hashCode());
        result = (prime * result) + ((threshold == null) ? 0 : threshold.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return "org.rhq.core.domain.alert.AlertCondition" + "[ " + "id=" + id + ", " + "category=" + category + ", "
            + "name=" + name + ", " + "comparator='" + comparator + "', " + "threshold=" + threshold + ", " + "option="
            + option + " ]";
    }
}