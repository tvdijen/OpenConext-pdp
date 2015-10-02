package pdp.repositories;


import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import pdp.PdpApplication;
import pdp.domain.PdpPolicy;

import java.util.List;

import static org.junit.Assert.assertEquals;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = PdpApplication.class)
@WebIntegrationTest(randomPort = true, value = {"xacml.properties.path=classpath:test.standalone.engine.xacml.properties", "spring.profiles.active=test"})
@Transactional //rollback commits
public class AbstractRepositoryTest {

  protected static final String NAME_ID = "name_id_";
  protected static final String POLICY_ID = "policy_id_";

  @Autowired
  protected PdpPolicyRepository pdpPolicyRepository;

  @Autowired
  protected PdpPolicyViolationRepository pdpPolicyViolationRepository;

}