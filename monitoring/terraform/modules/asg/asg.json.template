{
  "Resources": {
    "AutoScalingGroup": {
      "Type": "AWS::AutoScaling::AutoScalingGroup",
      "Properties": {
        "Cooldown": 300,
        "LaunchConfigurationName": "${launch_config_name}",
        "MaxSize": "${asg_max_size}",
        "DesiredCapacity": "${asg_desired_size}",
        "MinSize": "${asg_min_size}",
        "MetricsCollection": [
          {
            "Granularity": "1Minute",
            "Metrics": [
              "GroupMinSize",
              "GroupMaxSize",
              "GroupDesiredCapacity",
              "GroupInServiceInstances",
              "GroupPendingInstances",
              "GroupStandbyInstances",
              "GroupTerminatingInstances",
              "GroupTotalInstances"
            ]
          }
        ],
        "Tags" : [{
            "Key" : "Name",
            "Value" : "${asg_name}",
            "PropagateAtLaunch" : "true"
        }],
        "VPCZoneIdentifier": ${vpc_zone_identifier}
      },
      "UpdatePolicy": {
        "AutoScalingRollingUpdate": {
          "MinInstancesInService": "${asg_min_size}",
          "MaxBatchSize": "2",
          "PauseTime": "PT0S"
        }
      }
    }
  },
  "Outputs": {
    "AsgName": {
      "Description": "The name of the auto scaling group",
      "Value": {
        "Ref": "AutoScalingGroup"
      }
    }
  }
}
