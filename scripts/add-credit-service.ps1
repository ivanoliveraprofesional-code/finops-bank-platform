$ErrorActionPreference = "Stop"
$Root = Resolve-Path ".\backend"
$Service = "credit-risk-service"
$Package = "risk"
$Path = Join-Path $Root $Service

# 1. Crear Directorios
$Dirs = @(
    "src/main/java/com/finops/bank/$Package/domain/model",
    "src/main/java/com/finops/bank/$Package/domain/service",
    "src/main/java/com/finops/bank/$Package/infrastructure/grpc", # gRPC Adapter instead of Web
    "src/main/resources",
    "src/main/proto" # Directorio para archivos .proto
)

New-Item -ItemType Directory -Force -Path $Path | Out-Null
foreach ($d in $Dirs) { New-Item -ItemType Directory -Force -Path "$Path/$d" | Out-Null }

# 2. Crear POM Específico para gRPC
$PomContent = @"
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>com.finops.bank</groupId>
        <artifactId>finops-bank-platform</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>credit-risk-service</artifactId>

    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter</artifactId>
        </dependency>
        
        <dependency>
            <groupId>net.devh</groupId>
            <artifactId>grpc-server-spring-boot-starter</artifactId>
            <version>2.15.0.RELEASE</version>
        </dependency>
        
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-stub</artifactId>
            <version>1.60.0</version>
        </dependency>
        <dependency>
            <groupId>io.grpc</groupId>
            <artifactId>grpc-protobuf</artifactId>
            <version>1.60.0</version>
        </dependency>
        <dependency>
            <groupId>javax.annotation</groupId>
            <artifactId>javax.annotation-api</artifactId>
            <version>1.3.2</version>
        </dependency>
    </dependencies>

    <build>
        <extensions>
            <extension>
                <groupId>kr.motd.maven</groupId>
                <artifactId>os-maven-plugin</artifactId>
                <version>1.7.1</version>
            </extension>
        </extensions>
        <plugins>
            <plugin>
                <groupId>org.xolstice.maven.plugins</groupId>
                <artifactId>protobuf-maven-plugin</artifactId>
                <version>0.6.1</version>
                <configuration>
                    <protocArtifact>com.google.protobuf:protoc:3.25.1:exe:`${os.detected.classifier}</protocArtifact>
                    <pluginId>grpc-java</pluginId>
                    <pluginArtifact>io.grpc:protoc-gen-grpc-java:1.60.0:exe:`${os.detected.classifier}</pluginArtifact>
                </configuration>
                <executions>
                    <execution>
                        <goals>
                            <goal>compile</goal>
                            <goal>compile-custom</goal>
                        </goals>
                    </execution>
                </executions>
            </plugin>
        </plugins>
    </build>
</project>
"@
Set-Content -Path "$Path/pom.xml" -Value $PomContent -Encoding UTF8

# 3. Crear Application Class
$AppClass = @"
package com.finops.bank.risk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CreditRiskServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(CreditRiskServiceApplication.class, args);
    }
}
"@
Set-Content -Path "$Path/src/main/java/com/finops/bank/risk/CreditRiskServiceApplication.java" -Value $AppClass -Encoding UTF8

Write-Host "Credit Risk Service (gRPC) Created Successfully!" -ForegroundColor Green