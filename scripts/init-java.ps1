<#
.SYNOPSIS
    Genera el esqueleto Java 21 Spring Boot.
    FIXES:
    1. Maven Dependencies (Corregido)
    2. Encoding: Usa .NET puro para evitar el BOM (\ufeff) que rompe Java.
#>
$ErrorActionPreference = "Stop"

# --- CONFIGURACION ---
$GroupId = "com.finops.bank"
$Version = "1.0.0-SNAPSHOT"
$JavaVersion = "21"
# Versions
$SpringBootVersion = "3.2.3"
$SpringCloudVersion = "2023.0.0"
$SpringAwsVersion = "3.1.0"
$TestcontainersVersion = "1.19.6"
$MapstructVersion = "1.5.5.Final"
$LombokVersion = "1.18.30"

# Rutas
$ScriptPath = $PSScriptRoot
$ProjectRoot = Split-Path -Parent $ScriptPath
$BaseDir = Join-Path $ProjectRoot "backend"

# --- HELPER PARA ESCRIBIR SIN BOM ---
function Write-NoBom {
    param(
        [string]$Path,
        [string]$Content
    )
    # Crea el directorio si no existe (seguridad extra)
    $Dir = Split-Path -Parent $Path
    if (!(Test-Path $Dir)) { New-Item -ItemType Directory -Force -Path $Dir | Out-Null }
    
    # Escribe usando UTF-8 sin firma (No BOM)
    $Enc = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($Path, $Content, $Enc)
}

# Limpieza
if (Test-Path $BaseDir) { 
    Write-Host "Limpiando backend antiguo..." -ForegroundColor Yellow 
    Remove-Item $BaseDir -Recurse -Force
}

# Mapeo de Módulos
$ModulesMap = @{
    "auth-service" = "auth";
    "core-banking" = "core";
    "audit-service" = "audit"
}

# --- 1. ESTRUCTURA DE DIRECTORIOS ---
Write-Host "Creando estructura en $BaseDir..." -ForegroundColor Cyan
New-Item -ItemType Directory -Force -Path $BaseDir | Out-Null

foreach ($Key in $ModulesMap.Keys) {
    $PackageName = $ModulesMap[$Key]
    $ModulePath = Join-Path $BaseDir $Key
    $SrcPath = "$ModulePath/src/main/java/com/finops/bank/$PackageName"
    
    # Capas Hexagonales
    $Layers = @(
        "domain/model",
        "domain/exception",
        "application/port/in",
        "application/port/out",
        "application/service",
        "application/mapper",
        "infrastructure/adapter/in/web",
        "infrastructure/adapter/out/persistence/entity",
        "infrastructure/adapter/out/persistence/repository",
        "infrastructure/config"
    )

    foreach ($Layer in $Layers) {
        New-Item -ItemType Directory -Force -Path "$SrcPath/$Layer" | Out-Null
    }
    
    # Directorios extra
    New-Item -ItemType Directory -Force -Path "$ModulePath/src/test/java/com/finops/bank/$PackageName" | Out-Null
    New-Item -ItemType Directory -Force -Path "$ModulePath/src/main/resources" | Out-Null
    
    # Application.yml vacio
    Write-NoBom "$ModulePath/src/main/resources/application.yml" ""
}

# --- 2. POM PADRE (Fixed & Clean) ---
Write-Host "Generando POM Padre..." -ForegroundColor Cyan
$ParentPomContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>$GroupId</groupId>
    <artifactId>finops-bank-platform</artifactId>
    <version>$Version</version>
    <packaging>pom</packaging>

    <modules>
        <module>auth-service</module>
        <module>core-banking</module>
        <module>audit-service</module>
    </modules>

    <properties>
        <java.version>$JavaVersion</java.version>
        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
        <spring.boot.version>$SpringBootVersion</spring.boot.version>
        <spring.cloud.version>$SpringCloudVersion</spring.cloud.version>
        <spring.aws.version>$SpringAwsVersion</spring.aws.version>
        <testcontainers.version>$TestcontainersVersion</testcontainers.version>
        <mapstruct.version>$MapstructVersion</mapstruct.version>
        <lombok.version>$LombokVersion</lombok.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-dependencies</artifactId>
                <version>`${spring.boot.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>`${spring.cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>io.awspring.cloud</groupId>
                <artifactId>spring-cloud-aws-dependencies</artifactId>
                <version>`${spring.aws.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>`${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>

    <dependencies>
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>`${lombok.version}</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>org.mapstruct</groupId>
            <artifactId>mapstruct</artifactId>
            <version>`${mapstruct.version}</version>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-testcontainers</artifactId>
            <scope>test</scope>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>junit-jupiter</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.apache.maven.plugins</groupId>
                <artifactId>maven-compiler-plugin</artifactId>
                <version>3.11.0</version>
                <configuration>
                    <source>`${java.version}</source>
                    <target>`${java.version}</target>
                    <annotationProcessorPaths>
                        <path>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                            <version>`${lombok.version}</version>
                        </path>
                        <path>
                            <groupId>org.mapstruct</groupId>
                            <artifactId>mapstruct-processor</artifactId>
                            <version>`${mapstruct.version}</version>
                        </path>
                    </annotationProcessorPaths>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
"@
Write-NoBom "$BaseDir/pom.xml" $ParentPomContent

# --- 3. GENERAR POMS DE MÓDULOS ---
foreach ($Key in $ModulesMap.Keys) {
    $ModuleName = $Key
    Write-Host "Generando POM para $ModuleName..." -ForegroundColor Cyan
    
    $ModulePomContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>$GroupId</groupId>
        <artifactId>finops-bank-platform</artifactId>
        <version>$Version</version>
    </parent>

    <artifactId>$ModuleName</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.liquibase</groupId>
            <artifactId>liquibase-core</artifactId>
        </dependency>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>postgresql</artifactId>
            <scope>test</scope>
        </dependency>
"@

    if ($ModuleName -eq "auth-service") {
        $ModulePomContent += @"

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-security</artifactId>
        </dependency>
        <dependency>
            <groupId>io.awspring.cloud</groupId>
            <artifactId>spring-cloud-aws-starter-secrets-manager</artifactId>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-api</artifactId>
            <version>0.11.5</version>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-impl</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>io.jsonwebtoken</groupId>
            <artifactId>jjwt-jackson</artifactId>
            <version>0.11.5</version>
            <scope>runtime</scope>
        </dependency>
"@
    }

    $ModulePomContent += @"
    </dependencies>
</project>
"@
    Write-NoBom "$BaseDir/$ModuleName/pom.xml" $ModulePomContent
}

# --- 4. MAIN APPLICATION CLASS ---
foreach ($Key in $ModulesMap.Keys) {
    $PackageName = $ModulesMap[$Key]
    $ClassName = (Get-Culture).TextInfo.ToTitleCase($PackageName) + "ServiceApplication"
    $MainPath = "$BaseDir/$Key/src/main/java/com/finops/bank/$PackageName/$ClassName.java"
    
    $MainContent = @"
package com.finops.bank.$PackageName;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class $ClassName {
    public static void main(String[] args) {
        SpringApplication.run($ClassName.class, args);
    }
}
"@
    Write-NoBom $MainPath $MainContent
}

Write-Host ">>> ESTRUCTURA REGENERADA SIN BOM (UTF-8 CLEAN) <<<" -ForegroundColor Green