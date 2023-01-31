# cplex-java-worker (Beta)

This project demonstrates the development, testing, deployment and verification of a Decision Optimization Java worker in WML.
 
1. Install the development environment:
* [Install JAVA 11 JDK](https://developer.ibm.com/languages/java/semeru-runtimes/downloads).
* Install Apache Maven launching the `mvnw` command located in your project directory.
* [Install Conda or any Python 3.9](https://docs.conda.io/projects/conda/en/latest/user-guide/install/index.html) and add the Python executable file name to your PATH.
* [Install IBM Watson Machine Learning Python client](https://ibm-wml-api-pyclient.mybluemix.net/) with `pip install ibm-watson-machine-learning`.
* [Install IBM ILOG CPLEX Optimization Studio](https://www.ibm.com/products/ilog-cplex-optimization-studio) (The compilation and modeling can be done with the [Community Edition of CPLEX](https://www.ibm.com/account/reg/us-en/signup?formid=urx-20028) that is freely available.)

2. Set up the development environment:
This section describes how you will configure your maven pom.xml and settings.
* Set up the cplex_version property that you target in the pom.xml file and install the corresponding CPLEX or CP jar in your local maven repository. For example, if you target `cplex_version=20.1`, to install the cplex-jar in your maven local repository, use the following command: 
```
mvn install:install-file "-Dfile=%CPLEX_STUDIO_DIR%\cplex\lib\cplex.jar" -DgroupId=com.ibm.ilog.optim -DartifactId=cplex-jar -Dversion=20.1 -Dpackaging=jar
```
and to install the cpo-jar in your maven local repository:
```
mvn install:install-file "-Dfile=%CPLEX_STUDIO_DIR%\cpoptimizer\lib\ILOG.CP.jar" -DgroupId=com.ibm.ilog.optim -DartifactId=cpo-jar -Dversion=20.1 -Dpackaging=jar
```
* Accordingly, set up the cplex_library_path property to target the CPLEX or CPO native library for your OS.
* Now you can execute the commands `mvn compile` or `mvn test` in your project.

3. Set up your integration test environment:
If you want to verify the deployment to a WML cluster, you must specify the WML environment that you want to target during your tests.
* Update the wml_test_environment profile in your pom.xml or settings.xml file:
```
	<profiles>
		<profile>
			<id>wml_test_environment</id>
			<properties>
				<!--URL for `us-south` environment -->
				<wml_location>us-south.ml.cloud.ibm.com</wml_location>
				<wml_api_key>MY_APY_KEY</wml_api_key>

				<!-- BETA_FEATURE_KEY is provided to customers enrolled in beta program -->
				<wml_api_key>BETA_FEATURE_KEY</wml_api_key>
				
				<wml_space_id>4b3b6139-5827-490a-a205-c8097c5916f2</wml_space_id>
				<wml_deployment_size>S</wml_deployment_size>
			</properties>
		</profile>
	</profiles>
 ```
* Now you can execute the commands `mvn -P wml_test_environment verify` or `mvn -P wml_test_environment install` in your project.
 

 
