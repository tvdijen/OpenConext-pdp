package pdp.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.apache.commons.io.IOUtils;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;

import java.nio.charset.Charset;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static pdp.xacml.PdpPolicyDefinitionParser.IDP_ENTITY_ID;
import static pdp.xacml.PdpPolicyDefinitionParser.SP_ENTITY_ID;

public class JsonPolicyRequestTest {

    private static ObjectMapper objectMapper = new ObjectMapper();

    static {
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    @Test
    public void testJson() throws Exception {
        JsonPolicyRequest.Request request = new JsonPolicyRequest.Request();
        JsonPolicyRequest policyRequest = new JsonPolicyRequest(request);
        request.returnPolicyIdList = true;

        List<JsonPolicyRequest.Attribute> accessSubjectAttributes = request.accessSubject.attributes;
        accessSubjectAttributes.add(new JsonPolicyRequest.Attribute("urn:mace:terena" +
            ".org:attribute-def:schacHomeOrganization", "surfnet.nl"));
        accessSubjectAttributes.add(new JsonPolicyRequest.Attribute("urn:mace:terena.org:attribute-def:edu", "what"));
        accessSubjectAttributes.add(new JsonPolicyRequest.Attribute("urn:mace:terena.org:attribute-def:different",
            "surfnet.nl"));
        accessSubjectAttributes.add(new JsonPolicyRequest.Attribute
            ("urn:mace:dir:attribute-def:eduPersonAffiliation", "employee"));

        List<JsonPolicyRequest.Attribute> resourceAttributes = request.resource.attributes;
        resourceAttributes.add(new JsonPolicyRequest.Attribute(SP_ENTITY_ID, "avans_sp"));
        resourceAttributes.add(new JsonPolicyRequest.Attribute(IDP_ENTITY_ID, "avans_idp"));

        String json = objectMapper.writeValueAsString(policyRequest);
        String expected = IOUtils.toString(new ClassPathResource("xacml/requests/json_policy_request.json")
            .getInputStream(), Charset.defaultCharset());
        assertEquals(expected, json);
    }
}