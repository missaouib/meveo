package org.meveo.model.crm;

import java.io.Serializable;

import org.meveo.model.BusinessEntity;

public class EntityReferenceWrapper implements Serializable {

    private static final long serialVersionUID = -4756870628233941711L;

    public EntityReferenceWrapper() {
    }

    public EntityReferenceWrapper(BusinessEntity entity) {
        super();
        if (entity == null) {
            return;
        }
        classname = entity.getClass().getName();
        int pos = classname.indexOf("_$$_");
        if (pos > 0) {
            classname = classname.substring(0, pos);
        }

        code = entity.getCode();
    }

    public EntityReferenceWrapper(String classname, String code) {
        this.classname = classname;
        this.code = code;
    }

    private String classname;

    private String code;

    public String getClassname() {
        return classname;
    }

    public void setClassname(String classname) {
        this.classname = classname;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public boolean isEmpty() {
        return code == null;
    }

    @Override
    public String toString() {
        return String.format("EntityReferenceWrapper [classname=%s, code=%s]", classname, code);
    }
}
