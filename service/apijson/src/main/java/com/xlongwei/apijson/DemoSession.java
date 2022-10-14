package com.xlongwei.apijson;

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;

import com.alibaba.fastjson.annotation.JSONType;

@JSONType(includes = { "attributes" })
public class DemoSession implements HttpSession, Serializable {
    private Map<String, Object> attributes = new ConcurrentHashMap<>();

    @Override
    public long getCreationTime() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public String getId() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public long getLastAccessedTime() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public ServletContext getServletContext() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public int getMaxInactiveInterval() {
        throw new UnsupportedOperationException("");
    }

    @Deprecated
    @Override
    public javax.servlet.http.HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    @Override
    public Object getValue(String name) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public String[] getValueNames() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public void setAttribute(String name, Object value) {
        if (value != null)
            attributes.put(name, value);
    }

    @Override
    public void putValue(String name, Object value) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public void removeAttribute(String name) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public void removeValue(String name) {
        throw new UnsupportedOperationException("");
    }

    @Override
    public void invalidate() {
        throw new UnsupportedOperationException("");
    }

    @Override
    public boolean isNew() {
        throw new UnsupportedOperationException("");
    }

    /**
     * @return the attributes
     */
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    /**
     * @param attributes the attributes to set
     */
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

}
