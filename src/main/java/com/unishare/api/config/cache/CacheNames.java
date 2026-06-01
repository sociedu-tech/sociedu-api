package com.unishare.api.config.cache;

public final class CacheNames {

    public static final String MENTOR_PROFILE = "mentorProfiles";
    public static final String MENTOR_SEARCH = "mentorSearch";

    public static final String USER_PROFILE = "userProfiles";
    public static final String USER_FULL_PROFILE = "userFullProfiles";
    public static final String USER_EDUCATIONS = "userEducations";
    public static final String USER_LANGUAGES = "userLanguages";
    public static final String USER_EXPERIENCES = "userExperiences";
    public static final String USER_CERTIFICATES = "userCertificates";

    public static final String SERVICE_PACKAGE = "servicePackages";
    public static final String MENTOR_SERVICE_PACKAGES = "mentorServicePackages";
    public static final String ACTIVE_SERVICE_PACKAGES = "activeServicePackages";
    public static final String MY_SERVICE_PACKAGES = "myServicePackages";
    public static final String MY_SERVICE_PACKAGE = "myServicePackage";
    public static final String SERVICE_PACKAGE_VERSION = "servicePackageVersions";
    public static final String ACTIVE_PACKAGE_VERSION = "activePackageVersions";
    public static final String ACTIVE_PACKAGE_VERSIONS = "activePackageVersionsList";
    public static final String CURRICULUM = "curriculum";
    public static final String ACTIVE_CURRICULUM = "activeCurriculum";

    private CacheNames() {
    }

    public static String[] all() {
        return new String[]{
                MENTOR_PROFILE,
                MENTOR_SEARCH,
                USER_PROFILE,
                USER_FULL_PROFILE,
                USER_EDUCATIONS,
                USER_LANGUAGES,
                USER_EXPERIENCES,
                USER_CERTIFICATES,
                SERVICE_PACKAGE,
                MENTOR_SERVICE_PACKAGES,
                ACTIVE_SERVICE_PACKAGES,
                MY_SERVICE_PACKAGES,
                MY_SERVICE_PACKAGE,
                SERVICE_PACKAGE_VERSION,
                ACTIVE_PACKAGE_VERSION,
                ACTIVE_PACKAGE_VERSIONS,
                CURRICULUM,
                ACTIVE_CURRICULUM
        };
    }
}
