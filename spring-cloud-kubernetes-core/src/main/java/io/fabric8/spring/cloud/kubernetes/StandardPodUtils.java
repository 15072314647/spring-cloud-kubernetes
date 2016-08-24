/*
 *     Copyright (C) 2016 to the original authors.
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *             http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package io.fabric8.spring.cloud.kubernetes;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Paths;
import java.util.function.Supplier;

public class StandardPodUtils implements PodUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(StandardPodUtils.class);
    public static final String HOSTNAME = "HOSTNAME";

    private final KubernetesClient client;
    private final String hostName;
    private Supplier<Pod> current;

    public StandardPodUtils(KubernetesClient client) {
        this.client = client;
        this.hostName = System.getenv(HOSTNAME);
        this.current = LazilyInstantiate.using(() -> internalGetPod());
    }

    @Override
    public Supplier<Pod> currentPod() {
        return current;
    }

    @Override
    public Boolean isInsideKubernetes() {
        return currentPod().get() != null;
    }

    private synchronized Pod internalGetPod() {
        try {
            if (isServiceAccountFound() && isHostNameEnvVarPresent()) {
                return client.pods().withName(hostName).get();
            } else {
                return null;
            }
        } catch (Throwable t) {
            LOGGER.warn("Failed to get pod with name:[" + hostName + "]. You should look into this if things aren't working as you expect. Are you missing serviceaccount permissions?", t);
            return null;
        }
    }

    private boolean isHostNameEnvVarPresent() {
        return hostName != null && !hostName.isEmpty();
    }

    private boolean isServiceAccountFound() {
        return Paths.get(Config.KUBERNETES_SERVICE_ACCOUNT_TOKEN_PATH).toFile().exists() &&
                Paths.get(Config.KUBERNETES_SERVICE_ACCOUNT_CA_CRT_PATH).toFile().exists();
    }
}
