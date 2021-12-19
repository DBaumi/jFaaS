package jContainer.helper;

/**
 * constants for running this project are here defined, don't forget to change data in the credentials file and 'defaultPemName'
 */
public interface Constants {
    String region = "eu-central-1";

    interface CloudWatch {
        String log_group_name = "/" + CloudWatch.log_group_prefix + "/terraform_ecs_log";
        String log_group_prefix = "ecs";
        Integer retention_time_days = 1;
    }

    interface Docker {
        String hub_user_and_repo_name = CredentialsProperties.dockerUser + "/" + CredentialsProperties.dockerRepo + ":";
    }

    interface utils {
        Integer sleepTimer = 5000;
    }

    interface Paths {
        String resourceFolder = "src/main/resources/";
        String jarFilePath = "./jars/";
        String scriptFolder = Paths.resourceFolder + "scripts/";
        String terraformLocalDocker = Paths.resourceFolder + "localTerraform/";
        String googleCredentials = "google_cred.json";
        String localTerraformDocker = Paths.resourceFolder + "localTerraform/";
        String localFunctionDocker = Paths.resourceFolder + "localFunction/";

    }

    interface DockerImageTags {
        String openJDK_18 = "openjdk:18-oraclelinux7";
        String openJDK_11 = "openjdk:11-oraclelinux8";
        String openJDK_19_slim = "openjdk:19-slim";
        String alpineJavaJDK_8 = "anapsix/alpine-java:8u202b08_jdk";
        String eclipseTemurinJDK_11 = "eclipse-temurin:11.0.13_8-jre-focal";
    }
}
