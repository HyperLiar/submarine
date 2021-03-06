/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.submarine.server.submitter.yarnservice.tensorflow.component;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.yarn.service.api.records.Component;
import org.apache.hadoop.yarn.service.api.records.Component.RestartPolicyEnum;
import org.apache.submarine.client.cli.param.runjob.RunJobParameters;
import org.apache.submarine.client.cli.param.runjob.TensorFlowRunJobParameters;
import org.apache.submarine.commons.runtime.api.TensorFlowRole;
import org.apache.submarine.commons.runtime.fs.RemoteDirectoryManager;
import org.apache.submarine.server.submitter.yarnservice.AbstractComponent;
import org.apache.submarine.server.submitter.yarnservice.FileSystemOperations;
import org.apache.submarine.server.submitter.yarnservice.YarnServiceUtils;
import org.apache.submarine.server.submitter.yarnservice.command.TensorFlowLaunchCommandFactory;
import org.apache.submarine.server.submitter.yarnservice.tensorflow.TensorFlowCommons;
import org.apache.submarine.server.submitter.yarnservice.utils.DockerUtilities;
import org.apache.submarine.server.submitter.yarnservice.utils.SubmarineResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Objects;

/**
 * Component implementation for Tensorboard's Tensorboard.
 */
public class TensorBoardComponent extends AbstractComponent {
  private static final Logger LOG =
      LoggerFactory.getLogger(TensorBoardComponent.class);

  public static final String TENSORBOARD_QUICKLINK_LABEL = "Tensorboard";
  private static final int DEFAULT_PORT = 6006;

  //computed fields
  private String tensorboardLink;

  public TensorBoardComponent(FileSystemOperations fsOperations,
                              RemoteDirectoryManager remoteDirectoryManager,
                              RunJobParameters parameters,
                              TensorFlowLaunchCommandFactory launchCommandFactory,
                              Configuration yarnConfig) {
    super(fsOperations, remoteDirectoryManager, parameters,
        TensorFlowRole.TENSORBOARD, yarnConfig, launchCommandFactory);
  }

  @Override
  public Component createComponent() throws IOException {
    TensorFlowRunJobParameters tensorFlowParams =
        (TensorFlowRunJobParameters) this.parameters;

    Objects.requireNonNull(tensorFlowParams.getTensorboardResource(),
        "TensorBoard resource must not be null!");

    Component component = new Component();
    component.setName(role.getComponentName());
    component.setNumberOfContainers(1L);
    component.setRestartPolicy(RestartPolicyEnum.NEVER);
    component.setResource(SubmarineResourceUtils.convertYarnResourceToServiceResource(
        tensorFlowParams.getTensorboardResource()));

    if (tensorFlowParams.getTensorboardDockerImage() != null) {
      component.setArtifact(
          DockerUtilities.getDockerArtifact(tensorFlowParams.getTensorboardDockerImage()));
    }

    TensorFlowCommons.addCommonEnvironments(component, role);
    generateLaunchCommand(component);

    tensorboardLink = "http://" + YarnServiceUtils.getDNSName(
        parameters.getName(),
        role.getComponentName() + "-" + 0, TensorFlowCommons.getUserName(),
        TensorFlowCommons.getDNSDomain(yarnConfig), DEFAULT_PORT);
    LOG.info("Link to tensorboard:" + tensorboardLink);

    return component;
  }

  public String getTensorboardLink() {
    return tensorboardLink;
  }

}
