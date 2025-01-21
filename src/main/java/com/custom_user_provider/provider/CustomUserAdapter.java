package com.custom_user_provider.provider;
import com.custom_user_provider.entity.User;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;


public class CustomUserAdapter extends AbstractUserAdapterFederatedStorage {
    protected User entity;
    protected String keycloakId;

    public CustomUserAdapter(KeycloakSession session, RealmModel realm, ComponentModel model, User entity) {
        super(session, realm, model);
        this.entity = entity;
        keycloakId = StorageId.keycloakId(model, entity.getUserId());
    }

    @Override
    public String getUsername() {
        return entity.getUsername();
    }

    @Override
    public void setUsername(String username) {
        entity.setUsername(username);
    }

    @Override
    public void setEmail(String email) {
        entity.setEmail(email);
    }

    @Override
    public String getEmail() {
        return entity.getEmail();
    }

    @Override
    public String getId() {
        return keycloakId;
    }

    @Override
    public void setSingleAttribute(String name, String value) {
        switch (name) {
            case "email" -> entity.setEmail(value);
            case "firstName" -> entity.setFirstName(value);
            case "lastName" -> entity.setLastName(value);
            default -> super.setSingleAttribute(name, value);
        }
    }

    @Override
    public void removeAttribute(String name) {
        switch (name) {
            case "email" -> entity.setEmail(null);
            case "firstName" -> entity.setFirstName(null);
            case "lastName" -> entity.setLastName(null);
            default -> super.removeAttribute(name);
        }
    }

    @Override
    public void setAttribute(String name, List<String> values) {
        switch (name) {
            case "email" -> entity.setEmail(values.get(0));
            case "firstName" -> entity.setFirstName(values.get(0));
            case "lastName" -> entity.setLastName(values.get(0));
            default -> super.setAttribute(name, values);
        }
    }

    @Override
    public String getFirstAttribute(String name) {
        String result = null;
        switch (name) {
            case "email" -> result = entity.getEmail();
            case "firstName" -> result = entity.getFirstName();
            case "lastName" -> result = entity.getLastName();
            default -> result = super.getFirstAttribute(name);
        }
        return result;
    }

    @Override
    public Map<String, List<String>> getAttributes() {
        Map<String, List<String>> attrs = super.getAttributes();
        MultivaluedHashMap<String, String> all = new MultivaluedHashMap<>();
        all.putAll(attrs);
        all.add("email", entity.getEmail());
        all.add("firstName", entity.getFirstName());
        all.add("lastName", entity.getLastName());
        return all;
    }

    @Override
    public Stream<String> getAttributeStream(String name) {
        switch (name) {
            case "email" -> {
                List<String> email = new LinkedList<>();
                email.add(entity.getEmail());
                return email.stream();
            }
            case "firstName" -> {
                List<String> firstName = new LinkedList<>();
                firstName.add(entity.getFirstName());
                return firstName.stream();
            }
            case "lastName" -> {
                List<String> lastName = new LinkedList<>();
                lastName.add(entity.getLastName());
                return lastName.stream();
            }
            default -> {
                return super.getAttributeStream(name);
            }
        }
    }


}
