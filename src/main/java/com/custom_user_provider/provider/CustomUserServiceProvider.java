package com.custom_user_provider.provider;

import com.custom_user_provider.entity.User;
import com.custom_user_provider.repositories.UserRepository;
import org.keycloak.component.ComponentModel;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.credential.CredentialModel;
import org.keycloak.credential.hash.PasswordHashProvider;
import org.keycloak.models.*;
import org.keycloak.models.credential.PasswordCredentialModel;
import org.keycloak.storage.StorageId;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.user.UserLookupProvider;
import org.keycloak.storage.user.UserQueryProvider;
import org.keycloak.storage.user.UserRegistrationProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

public class CustomUserServiceProvider implements UserStorageProvider, UserLookupProvider,
        UserRegistrationProvider,
        UserQueryProvider,
        CredentialInputValidator
{
    private static final Logger log = LoggerFactory.getLogger(CustomUserServiceProvider.class);
    protected UserRepository userRepository;
    protected ComponentModel model;
    protected KeycloakSession session;

    // Constructor: Khởi tạo session Keycloak và repository để kết nối database.
    public CustomUserServiceProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;
        this.userRepository = new UserRepository(session.getProvider(JpaConnectionProvider.class,
                "keycloak-provider").getEntityManager());
    }


    @Override
    public boolean supportsCredentialType(String s) {
//        s: credential type
        return PasswordCredentialModel.TYPE.endsWith(s);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String s) {

        return getPassword(realmModel, userModel) != null;
    }
    public PasswordCredentialModel getPassword(RealmModel realm, UserModel user) {
        List<CredentialModel> passwords = user.credentialManager().getStoredCredentialsByTypeStream(PasswordCredentialModel.TYPE).toList();
        if (passwords.isEmpty()) return null;
        return PasswordCredentialModel.createFromCredentialModel(passwords.get(0));
    }

    @Override
    public boolean isValid(RealmModel realm, UserModel user, CredentialInput input) {
        if (!(input instanceof UserCredentialModel)) {
            System.out.println("Expected instance of UserCredentialModel for CredentialInput");
            return false;

        }
        if (input.getChallengeResponse() == null) {
            System.out.println(String.format("Input password was null for user %s", user.getUsername()));
            return false;
        }
        PasswordCredentialModel password = getPassword(realm, user);
        if (password == null) {
            System.out.println(String.format("No password stored for user %s", user.getUsername()));
            return false;
        }
        PasswordHashProvider hash = session.getProvider(PasswordHashProvider.class, password.getPasswordCredentialData().getAlgorithm());
        if (hash == null) {
            System.out.println(String.format("PasswordHashProvider %s not found for user %s ", password.getPasswordCredentialData().getAlgorithm(), user.getUsername()));
            return false;
        }
        try {
            if (!hash.verify(input.getChallengeResponse(), password)) {
                System.out.println(String.format("Failed password validation for user %s ", user.getUsername()));
                return false;
            }

            rehashPasswordIfRequired(session, realm, user, input, password);
        } catch (Throwable t) {
            System.out.println("Error when validating user password");
            System.out.println(t.getCause().toString());
            return false;
        }

        return true;
    }

    private void rehashPasswordIfRequired(KeycloakSession session, RealmModel realm, UserModel user, CredentialInput input, PasswordCredentialModel password) {
        PasswordPolicy passwordPolicy = realm.getPasswordPolicy();
        PasswordHashProvider provider;
        if (passwordPolicy != null && passwordPolicy.getHashAlgorithm() != null) {
            provider = session.getProvider(PasswordHashProvider.class, passwordPolicy.getHashAlgorithm());
        } else {
            provider = session.getProvider(PasswordHashProvider.class);
        }

        if (!provider.policyCheck(passwordPolicy, password)) {
            int iterations = passwordPolicy != null ? passwordPolicy.getHashIterations() : -1;

            PasswordCredentialModel newPassword = provider.encodedCredential(input.getChallengeResponse(), iterations);
            newPassword.setId(password.getId());
            newPassword.setCreatedDate(password.getCreatedDate());
            newPassword.setUserLabel(password.getUserLabel());
            user.credentialManager().updateStoredCredential(newPassword);
        }
    }



    @Override
    public UserModel getUserById(RealmModel realmModel, String userId) {
        // Trích xuất persistenceId
        String persistenceId = StorageId.externalId(userId);
        System.out.println("Original userId: " + userId);
        System.out.println("Extracted persistenceId: " + persistenceId);

        // Tìm user trong database
        Optional<User> optionalProfile = userRepository.findByProfileId(persistenceId);

        if (optionalProfile.isEmpty()) {
            System.out.println("No profile found for user id: " + userId + " (this may happen if the user was deleted)");
            return null; // Trả về null thay vì ném ngoại lệ
        }

        User p = optionalProfile.get();
        return new CustomUserAdapter(session, realmModel, model, p);
    }

    @Override
    public UserModel getUserByUsername(RealmModel realmModel, String username) {
        Optional<User> optionalProfile = userRepository.findByUsername(username);
        // Kiểm tra nếu không tìm thấy
        if (optionalProfile.isEmpty()) {
            return null;
        }
        // Trả về UserAdapter nếu tìm thấy
        User p = optionalProfile.get();
        return new CustomUserAdapter(session, realmModel, model, p);
    }

    @Override
    public UserModel getUserByEmail(RealmModel realmModel, String email) {
        //check user by mail
        Optional<User> existingUserOpt = userRepository.findByEmail(email);

       //không null tra ve CustomUserAdapter
        if (existingUserOpt.isPresent()) {
            User existingUser = existingUserOpt.get();
            return new CustomUserAdapter(session, realmModel, model, existingUser);
        }
        return null;
    }

    @Override
    public Stream<UserModel> searchForUserStream(RealmModel realm, Map<String, String> params, Integer firstResult, Integer maxResults) {
        String search = params.get(UserModel.SEARCH);
        String lowerCaseSearch = search!= null ? search.toLowerCase() : "";
        List<User> profiles = userRepository.findBySearchTerm(lowerCaseSearch, firstResult, maxResults);
        return profiles.stream().map(p -> new CustomUserAdapter(session, realm, model, p));
    }

    @Override
    public Stream<UserModel> getGroupMembersStream(RealmModel realmModel, GroupModel groupModel, Integer integer, Integer integer1) {
        return Stream.empty();
    }

    @Override
    public Stream<UserModel> searchForUserByUserAttributeStream(RealmModel realmModel, String s, String s1) {
        return Stream.empty();
    }


    @Override
    public UserModel addUser(RealmModel realm, String username) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setFirstName("FirstName");
        newUser.setLastName("LastName");
        newUser.setDob(LocalDate.now());
        // save
        userRepository.createUser(newUser);
        //trả về cho Keycloak
        return new CustomUserAdapter(session, realm, model,newUser);
    }

    @Override
    public boolean removeUser(RealmModel realmModel, UserModel user) {
        String persistenceId = StorageId.externalId(user.getId());
        boolean result = userRepository.deleteById(persistenceId);

        if (result) {
            System.out.println("Successfully removed user with ID: " + persistenceId);
        } else {
            System.out.println("Failed to remove user with ID: " + persistenceId);
        }
        return result;
    }

    @Override
    public int getUsersCount(RealmModel realm) {
        return userRepository.count();
    }

    @Override
    public void close() {
    }
}
