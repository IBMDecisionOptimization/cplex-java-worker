import argparse
import configparser
from os.path import exists

from ibm_watson_machine_learning import APIClient

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Java worker deploy in WML server")
    parser.add_argument("-l", "--location", help="WML server location", required=True)
    parser.add_argument(
        "-k",
        "--api_key",
        help="WML server api key used for Cloud",
        required=False,
        nargs="?",
    )
    parser.add_argument(
        "-u",
        "--username",
        help="WML server username used for CP4D",
        required=False,
        nargs="?",
    )
    parser.add_argument(
        "-p",
        "--password",
        help="WML server password used for CP4D",
        required=False,
        nargs="?",
    )
    parser.add_argument(
        "-s", "--space_id", help="WML space id used for deployment", required=True
    )
    parser.add_argument(
        "-n", "--name", help="WML model and deployment name", default="Java"
    )
    parser.add_argument("-t", "--tsize", help="WML deployment tshirt size", default="S")
    parser.add_argument("-c", "--config", help="WML deployments config")
    parser.add_argument(
        "-v", "--version", help="Decision Optimization version", default="20.1"
    )
    parser.add_argument(
        "-w", "--worker_class", help="Decision Optimization worker class"
    )
    parser.add_argument(
        "-f", "--file", help="Decision Optimization model tar gz", required=True
    )
    args = parser.parse_args()
    config = configparser.ConfigParser()

    if args.config is not None:
        if exists(args.config):
            config.read(args.config)
            if args.name in config:
                print(
                    "Model and deployment "
                    + args.name
                    + " already present in config deleting them..."
                )
                try:
                    wml_credentials = {
                        "url": "https://" + config[args.name]["location"]
                    }
                    if args.api_key is not None:
                        wml_credentials["apikey"] = args.api_key
                    else:
                        wml_credentials["username"] = args.username
                        wml_credentials["password"] = args.password
                        wml_credentials["instance_id"] = "openshift"
                        wml_credentials["version"] = "4.0"
                    client = APIClient(wml_credentials)
                    client.set.default_space(config[args.name]["space_id"])
                    client.deployments.delete(config[args.name]["deployment_uid"])
                    client.repository.delete(config[args.name]["model_uid"])
                except:
                    print("WML error deleting model and deployment " + args.name)
                finally:
                    config.remove_section(args.name)

    wml_credentials = {"url": "https://" + args.location}
    if args.api_key is not None:
        wml_credentials["apikey"] = args.api_key
    else:
        wml_credentials["username"] = args.username
        wml_credentials["password"] = args.password
        wml_credentials["instance_id"] = "openshift"
        wml_credentials["version"] = "4.0"
    client = APIClient(wml_credentials)
    client.set.default_space(args.space_id)

    # Allows to define default job settings
    default_solve_parameters = {
        "decision_optimization": {"oaas.logTailEnabled": "true"}
    }
    if args.worker_class is not None:
        print("Deploying using following worker class: " + args.worker_class)
        default_solve_parameters["decision_optimization"][
            "oaas.java.workerClass"
        ] = args.worker_class
    mnist_metadata = {
        client.repository.ModelMetaNames.NAME: args.name,
        client.repository.ModelMetaNames.DESCRIPTION: args.name + " model",
        client.repository.ModelMetaNames.TYPE: "do-cplex_" + args.version,
        client.repository.ModelMetaNames.SOFTWARE_SPEC_UID: client.software_specifications.get_uid_by_name(
            "do_" + args.version
        ),
        client.repository.ModelMetaNames.CUSTOM: default_solve_parameters,
    }
    model_details = client.repository.store_model(
        model=args.file, meta_props=mnist_metadata
    )
    model_uid = client.repository.get_model_id(model_details)
    print("model_uid: " + model_uid)

    meta_props = {
        client.deployments.ConfigurationMetaNames.NAME: args.name,
        client.deployments.ConfigurationMetaNames.DESCRIPTION: args.name
        + " model deployment",
        client.deployments.ConfigurationMetaNames.BATCH: {},
        client.deployments.ConfigurationMetaNames.HARDWARE_SPEC: {
            "name": args.tsize,
            "nodes": 1,
        },
    }

    deployment_details = client.deployments.create(model_uid, meta_props=meta_props)
    deployment_uid = client.deployments.get_uid(deployment_details)

    print("deployment_uid: " + deployment_uid)

    if args.config is not None:
        if exists(args.config):
            config.read(args.config)
        config[args.name] = {
            "model_uid": model_uid,
            "location": args.location,
            "space_id": args.space_id,
            "deployment_uid": deployment_uid,
        }
        with open(args.config, "w") as configfile:
            config.write(configfile)
        print("Deployment config saved in: " + args.config)
