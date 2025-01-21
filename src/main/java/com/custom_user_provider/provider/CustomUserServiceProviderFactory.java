package com.custom_user_provider.provider;

import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.storage.UserStorageProviderFactory;

public class CustomUserServiceProviderFactory implements UserStorageProviderFactory<CustomUserServiceProvider> {
    public static final String PROVIDER_ID = "custom-user-storage-database";

    @Override
    public CustomUserServiceProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        return new CustomUserServiceProvider(keycloakSession, componentModel);
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public void close() {
        UserStorageProviderFactory.super.close();
    }
}
