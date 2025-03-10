package io.jenkins.plugins.casc.core;

import static io.jenkins.plugins.casc.misc.Util.getJenkinsRoot;
import static io.jenkins.plugins.casc.misc.Util.toYamlString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import hudson.Extension;
import hudson.model.Descriptor;
import hudson.model.Node;
import hudson.model.TaskListener;
import hudson.slaves.AbstractCloudComputer;
import hudson.slaves.AbstractCloudSlave;
import hudson.slaves.EphemeralNode;
import io.jenkins.plugins.casc.ConfigurationAsCode;
import io.jenkins.plugins.casc.ConfigurationContext;
import io.jenkins.plugins.casc.ConfiguratorRegistry;
import io.jenkins.plugins.casc.misc.ConfiguredWithCode;
import io.jenkins.plugins.casc.misc.JenkinsConfiguredWithCodeRule;
import io.jenkins.plugins.casc.misc.junit.jupiter.WithJenkinsConfiguredWithCode;
import io.jenkins.plugins.casc.model.CNode;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.PretendSlave;

@WithJenkinsConfiguredWithCode
class JenkinsConfiguratorCloudSupportTest {

    @Test
    @ConfiguredWithCode("JenkinsConfiguratorCloudSupportTest.yml")
    void should_have_nodes_configured(JenkinsConfiguredWithCodeRule j) {
        assertEquals(2, j.jenkins.getNodes().size(), "Base nodes not found");
    }

    @Test
    void should_remove_normal_nodes_configured_after_reload(JenkinsConfiguredWithCodeRule j) throws Exception {
        final Node slave = new StaticPretendSlave();
        j.jenkins.addNode(slave);

        ConfigurationAsCode.get()
                .configure(this.getClass()
                        .getResource("JenkinsConfiguratorCloudSupportTest.yml")
                        .toString());
        assertEquals(2, j.jenkins.getNodes().size(), "Base nodes not found");
    }

    @Test
    void should_keep_cloud_no_instantiable_nodes_configured_after_reload(JenkinsConfiguredWithCodeRule j)
            throws Exception {
        final Node slave = new Cloud1PretendSlave();
        j.jenkins.addNode(slave);

        ConfigurationAsCode.get()
                .configure(this.getClass()
                        .getResource("JenkinsConfiguratorCloudSupportTest.yml")
                        .toString());
        assertEquals(3, j.jenkins.getNodes().size(), "Cloud nodes not found");
        assertNotNull(j.jenkins.getNode("agent1"), "Slave 1");
        assertNotNull(j.jenkins.getNode("agent2"), "Slave 1");
        assertNotNull(j.jenkins.getNode("testCloud"), "Slave cloud");
    }

    @Test
    void should_keep_cloud_ephemeral_nodes_configured_after_reload(JenkinsConfiguredWithCodeRule j) throws Exception {
        final Node slave = new Cloud2PretendSlave();
        j.jenkins.addNode(slave);

        ConfigurationAsCode.get()
                .configure(this.getClass()
                        .getResource("JenkinsConfiguratorCloudSupportTest.yml")
                        .toString());
        assertEquals(3, j.jenkins.getNodes().size(), "Cloud nodes not found");
        assertNotNull(j.jenkins.getNode("agent1"), "Slave 1");
        assertNotNull(j.jenkins.getNode("agent2"), "Slave 1");
        assertNotNull(j.jenkins.getNode("testCloud"), "Slave cloud");
    }

    @Test
    void should_keep_cloud_abstractCloudSlave_nodes_configured_after_reload(JenkinsConfiguredWithCodeRule j)
            throws Exception {
        final Node slave = new Cloud3PretendSlave();
        j.jenkins.addNode(slave);

        ConfigurationAsCode.get()
                .configure(this.getClass()
                        .getResource("JenkinsConfiguratorCloudSupportTest.yml")
                        .toString());
        assertEquals(3, j.jenkins.getNodes().size(), "Cloud nodes not found");
        assertNotNull(j.jenkins.getNode("agent1"), "Slave 1");
        assertNotNull(j.jenkins.getNode("agent2"), "Slave 1");
        assertNotNull(j.jenkins.getNode("testCloud"), "Slave cloud");
    }

    @Test
    @ConfiguredWithCode("JenkinsConfiguratorCloudSupportTest.yml")
    void should_export_only_static_nodes(JenkinsConfiguredWithCodeRule j) throws Exception {
        j.jenkins.addNode(new Cloud1PretendSlave());

        ConfiguratorRegistry registry = ConfiguratorRegistry.get();
        ConfigurationContext context = new ConfigurationContext(registry);
        final CNode configNode = getJenkinsRoot(context).get("nodes");

        final String yamlConfig = toYamlString(configNode);
        assertThat(yamlConfig, containsString("name: \"agent1\""));
        assertThat(yamlConfig, containsString("name: \"agent2\""));
        assertThat(yamlConfig, not(containsString("name: \"testCloud\"")));
    }

    private static class BasePretendSlave extends PretendSlave {
        public BasePretendSlave() throws IOException, Descriptor.FormException {
            super("testCloud", "remoteFS", 3, Mode.NORMAL, "labelString", null, null);
        }
    }

    private static class StaticPretendSlave extends BasePretendSlave {
        public StaticPretendSlave() throws IOException, Descriptor.FormException {
            super();
        }
    }

    private static class Cloud1PretendSlave extends StaticPretendSlave {

        public Cloud1PretendSlave() throws IOException, Descriptor.FormException {
            super();
        }

        @Extension
        public static class DescriptorImpl extends SlaveDescriptor {
            public boolean isInstantiable() {
                return false;
            }
        }
    }

    private static class Cloud2PretendSlave extends StaticPretendSlave implements EphemeralNode {

        public Cloud2PretendSlave() throws IOException, Descriptor.FormException {
            super();
        }

        @Override
        public Node asNode() {
            return null;
        }
    }

    private static class Cloud3PretendSlave extends AbstractCloudSlave {

        public Cloud3PretendSlave() throws IOException, Descriptor.FormException {
            super("testCloud", "remoteFS", null);
        }

        @Override
        public AbstractCloudComputer createComputer() {
            return null;
        }

        @Override
        protected void _terminate(TaskListener taskListener) {
            // empty
        }
    }
}
