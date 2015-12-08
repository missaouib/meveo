package org.meveo.export;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.meveo.model.IEntity;

/**
 * Export/import process template
 * 
 * @author Andrius Karpavicius
 */
public class ExportTemplate {

    private String ref;

    private String name;

    private Class<? extends IEntity> entityToExport;

    private Map<String, String> parameters;

    /**
     * A list of classes that should be exported with all attributes
     */
    private List<Class<? extends IEntity>> classesToExportAsFull = new ArrayList<Class<? extends IEntity>>();

    /**
     * A list of classes that should be exported in a short version - only ID attribute
     */
    private List<Class<? extends IEntity>> classesToExportAsId = new ArrayList<Class<? extends IEntity>>();

    /**
     * A list of classes that should not raise an exception if foreign key to entity of these classes was not found and import was explicitly requested to validate foreign keys
     */
    private List<Class<? extends IEntity>> classesToIgnoreFKNotFound = new ArrayList<Class<? extends IEntity>>();

    private List<RelatedEntityToExport> relatedEntities;

    /**
     * Other export/import templates grouped under this template
     */
    private List<ExportTemplate> groupedTemplates = new ArrayList<ExportTemplate>();

    private boolean canDeleteAfterExport = false;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<? extends IEntity> getEntityToExport() {
        return entityToExport;
    }

    public void setEntityToExport(Class<? extends IEntity> entityToExport) {
        this.entityToExport = entityToExport;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public List<Class<? extends IEntity>> getClassesToExportAsFull() {
        return classesToExportAsFull;
    }

    public void setClassesToExportAsFull(List<Class<? extends IEntity>> classesToExportAsFull) {
        this.classesToExportAsFull = classesToExportAsFull;
    }

    public String getClassesToExportAsFullTxt() {
        String classes = "";
        if (classesToExportAsFull != null) {
            for (Class<? extends IEntity> clazz : classesToExportAsFull) {
                classes = classes + (classes.length() == 0 ? "" : ", ") + clazz.getName();
            }
        }
        return classes;
    }

    public List<Class<? extends IEntity>> getClassesToExportAsId() {
        return classesToExportAsId;
    }

    public void setClassesToExportAsId(List<Class<? extends IEntity>> classesToExportAsId) {
        this.classesToExportAsId = classesToExportAsId;
    }

    public List<Class<? extends IEntity>> getClassesToIgnoreFKNotFound() {
        return classesToIgnoreFKNotFound;
    }

    public void setClassesToIgnoreFKNotFound(List<Class<? extends IEntity>> classesToIgnoreFKNotFound) {
        this.classesToIgnoreFKNotFound = classesToIgnoreFKNotFound;
    }

    public List<ExportTemplate> getGroupedTemplates() {
        return groupedTemplates;
    }

    public void setGroupedTemplates(List<ExportTemplate> groupedTemplates) {
        this.groupedTemplates = groupedTemplates;
    }

    public boolean isHasParameters() {
        return parameters != null && !parameters.isEmpty();
    }

    public boolean isCanDeleteAfterExport() {
        return canDeleteAfterExport;
    }

    public void setCanDeleteAfterExport(boolean canDeleteAfterExport) {
        this.canDeleteAfterExport = canDeleteAfterExport;
    }

    public List<RelatedEntityToExport> getRelatedEntities() {
        return relatedEntities;
    }

    public void setRelatedEntities(List<RelatedEntityToExport> relatedEntities) {
        this.relatedEntities = relatedEntities;
    }

    public void addRelatedEntity(String selection, Map<String, String> parameters) {
        if (relatedEntities == null) {
            relatedEntities = new ArrayList<RelatedEntityToExport>();
        }
        relatedEntities.add(new RelatedEntityToExport(selection, parameters)); 
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }

    public boolean isGroupedTemplate() {
        return groupedTemplates != null && !groupedTemplates.isEmpty();
    }

    @Override
    public String toString() {
        final int maxLen = 10;
        return String
            .format(
                "ExportTemplate [ref=%s, name=%s, entityToExport=%s, parameters=%s, classesToExportAsFull=%s, classesToExportAsId=%s, classesToIgnoreFKNotFound=%s, relatedEntities=%s, groupedTemplates=%s, canDeleteAfterExport=%s]",
                ref, name, entityToExport, parameters != null ? toString(parameters.entrySet(), maxLen) : null,
                classesToExportAsFull != null ? toString(classesToExportAsFull, maxLen) : null, classesToExportAsId != null ? toString(classesToExportAsId, maxLen) : null,
                classesToIgnoreFKNotFound != null ? toString(classesToIgnoreFKNotFound, maxLen) : null, relatedEntities != null ? toString(relatedEntities, maxLen) : null,
                groupedTemplates != null ? toString(groupedTemplates, maxLen) : null, canDeleteAfterExport);
    }

    private String toString(Collection<?> collection, int maxLen) {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        int i = 0;
        for (Iterator<?> iterator = collection.iterator(); iterator.hasNext() && i < maxLen; i++) {
            if (i > 0)
                builder.append(", ");
            builder.append(iterator.next());
        }
        builder.append("]");
        return builder.toString();
    }
}