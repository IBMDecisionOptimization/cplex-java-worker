import sys
from os.path import exists
import argparse
import configparser
from time import sleep
import base64
from ibm_watson_machine_learning import APIClient

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description='Java worker verification in WML server')
    parser.add_argument('-k','--api_key', help='WML server api key', required=True)
    parser.add_argument('-n','--name', help='WML model and deployment name', default="Java")
    parser.add_argument('-c','--config', help='WML deployments config', required=True)
    parser.add_argument('-w','--worker_class', help='Decision Optimization worker class')
    args = parser.parse_args()
    
    config = configparser.ConfigParser()
    if exists( args.config):
        config.read(args.config)
    else:
        sys.exit( "WML deployments config file does not exist")

    wml_credentials = {
        "apikey": args.api_key,
        "url": 'https://' + config[args.name]['location']
    }
    client = APIClient(wml_credentials)
    client.set.default_space(config[args.name]['space_id'])

    default_solve_parameters = {
        'oaas.logAttachmentName': 'log.txt'
    }
    if args.worker_class is not None:
        default_solve_parameters["oaas.java.workerClass"] = args.worker_class
    solve_payload = {
        client.deployments.DecisionOptimizationMetaNames.SOLVE_PARAMETERS: default_solve_parameters,
        client.deployments.DecisionOptimizationMetaNames.INPUT_DATA: [],
        client.deployments.DecisionOptimizationMetaNames.OUTPUT_DATA: [
            {
                "id":".*\.csv"
            },
            {
                "id":"log.txt"
            }
        ]
    }
    job_details = client.deployments.create_job(config[args.name]['deployment_uid'], solve_payload)
    job_uid = client.deployments.get_job_uid(job_details)
    while job_details['entity']['decision_optimization']['status']['state'] not in ['completed', 'failed', 'canceled']:
        print(job_details['entity']['decision_optimization']['status']['state'] + '...')
        sleep(5)
        job_details=client.deployments.get_job_details(job_uid)
    print(job_details)
    for output_data in job_details['entity']['decision_optimization']['output_data']:
        if output_data['id'] == 'log.txt':
            output = output_data['content']
            output = output.encode("UTF-8")
            output = base64.b64decode(output)
            output = output.decode("UTF-8")
            print(output)
    assert job_details['entity']['decision_optimization']['status']['state'] == 'completed'