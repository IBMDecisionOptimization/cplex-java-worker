# cplex-java-worker

This project demonstrates the development, testing, deployment and verification of a Decision Optimization Java worker in Watson Machine Learning in the following products:
* Cloud Pak for Data as a Service,
* Watsonx.ai,
* Cloud Pak for Data 4.8.x or later.

## 1. Install the development environment:
  - [Install JAVA 11 JDK](https://developer.ibm.com/languages/java/semeru-runtimes/downloads).
  - [Install Apache Maven](https://maven.apache.org/install.html) for mvn command.
  - [Install Conda or any Python](https://docs.conda.io/projects/conda/en/latest/user-guide/install/index.html) and add the Python executable file name to your PATH.
  - [Install IBM Watson Machine Learning Python client](https://ibm.github.io/watson-machine-learning-sdk/install.html) with `pip install ibm-watson-machine-learning`.
  - [Install IBM ILOG CPLEX Optimization Studio](https://www.ibm.com/products/ilog-cplex-optimization-studio) (The compilation and modeling can be done with the [Community Edition of CPLEX](https://www.ibm.com/account/reg/us-en/signup?formid=urx-20028) that is freely available.)

## 2. Set up the development environment:
This section describes how you will configure your maven pom.xml and settings.
-  Set up the cplex_version property that you target in the pom.xml file and install the corresponding CPLEX or CP jar in your local maven repository. For example, if you target `cplex_version=22.1`, to install the cplex-jar in your maven local repository, use the following command: 
```
mvn install:install-file "-Dfile=%CPLEX_STUDIO_DIR%\cplex\lib\cplex.jar" -DgroupId=com.ibm.ilog.optim -DartifactId=cplex-jar -Dversion=22.1 -Dpackaging=jar
```
and to install the cpo-jar in your maven local repository:
```
mvn install:install-file "-Dfile=%CPLEX_STUDIO_DIR%\cpoptimizer\lib\ILOG.CP.jar" -DgroupId=com.ibm.ilog.optim -DartifactId=cpo-jar -Dversion=22.1 -Dpackaging=jar
```
  - Accordingly, set up the cplex_library_path property to target the CPLEX or CPO native library for your OS.
  - Now you can execute the commands `mvn compile` or `mvn test` in your project directory.

## 3. Set up your integration test environment:
If you want to verify the deployment to a Watson Machine Learning cluster, you must specify the Watson Machine Learning environment that you want to target during your tests.
  - Update the profiles in your pom.xml or settings.xml file for your needs:
```
	<profiles>
		<profile>
			<id>wml_test_environment_cloud</id>
			<!-- https://dataplatform.cloud.ibm.com/docs/content/wsj/analyze-data/ml-authentication.html?context=cpdaas -->
			<properties>
				<wml_location>CLOUD_WML_CLUSTER</wml_location>
				<wml_api_key>MY_APY_KEY</wml_api_key>
				<wml_space_id>MY_SPACE_ID</wml_space_id>
				<wml_deployment_size>S</wml_deployment_size>
			</properties>
		</profile>
		<profile>
			<id>wml_test_environment_cp4d</id>
			<!-- https://www.ibm.com/docs/en/cloud-paks/cp-data/4.7.x?topic=deployments-authenticating-programmatic-access#outside-auth -->
			<properties>
				<wml_location>CP4D_WML_CLUSTER</wml_location>
				<wml_username>MY_USERNAME</wml_username>
				<wml_password>MY_PASSOWRD</wml_password>
				<wml_space_id>MY_SPACE_ID</wml_space_id>
				<wml_deployment_size>S</wml_deployment_size>
			</properties>
		</profile>
	</profiles>
 ```
  - Now you can execute the commands `mvn -P <profile_name> verify` or `mvn -P <profile_name> install` in your project, where `<profile_name>` is `wml_test_environment_cloud` or `wml_test_environment_cp4d`.
 

 
