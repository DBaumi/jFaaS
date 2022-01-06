package jContainer.helper;

/**
 * constants for running this project are here defined, don't forget to change data in the credentials file and 'defaultPemName'
 */
public interface Constants {
    String region = "eu-central-1";
    String aws_cmd = System.getProperty("os.name").contains("Windows") ? "aws.exe" : "aws";

    interface CloudWatch {
        String log_group_name = "/" + Constants.CloudWatch.log_group_prefix + "/terraform_ecs_log";
        String log_group_prefix = "ecs";
        Integer retention_time_days = 1;
    }

    interface Terraform {
        String local_terraform = "local-terraform";
    }

    interface Docker {
        String hub_user_and_repo_name = CredentialsProperties.dockerUser + "/" + CredentialsProperties.dockerRepo + ":";
    }

    interface utils {
        Integer sleepTimer = 5000;
    }

    interface Paths {
        String credentialsFile = "credentials.properties";
        String jContainerResourceFolder = "jContainer_resources/";
        String fallbackJarFolder = "./jars/";
        String scriptFolder = Constants.Paths.jContainerResourceFolder + "scripts/";
        String googleCredentials = "google_cred.json";
        String localTerraformDocker = Paths.jContainerResourceFolder + "localTerraform";
        String localFunctionDocker = Paths.jContainerResourceFolder + "localFunction/";

    }

    interface DockerImageTags {
        String openJDK_18 = "openjdk:18-oraclelinux7";
        String openJDK_11 = "openjdk:11-oraclelinux8";
        String openJDK_19_slim = "openjdk:19-slim";
        String alpineJavaJDK_8 = "anapsix/alpine-java:8u202b08_jdk";
        String eclipseTemurinJDK_11 = "eclipse-temurin:11.0.13_8-jre-focal";
    }
}
