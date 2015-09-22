package pdp;

import org.apache.openaz.xacml.api.Advice;
import org.apache.openaz.xacml.api.Request;
import org.apache.openaz.xacml.api.Response;
import org.apache.openaz.xacml.api.Result;
import org.apache.openaz.xacml.api.pdp.PDPEngine;
import org.apache.openaz.xacml.std.json.JSONRequest;
import org.apache.openaz.xacml.std.json.JSONResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import pdp.xacml.PDPEngineHolder;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.apache.openaz.xacml.api.Decision.DENY;

@RestController
public class PdpController {

  private static Logger LOG = LoggerFactory.getLogger(PdpController.class);
  private final PDPEngineHolder pdpEngineHolder;
  private PDPEngine pdpEngine;
  private PdpPolicyViolationRepository pdpPolicyViolationRepository;
  private ReadWriteLock lock = new ReentrantReadWriteLock();


  @Autowired
  public PdpController(@Value("${initial.delay.policies.refresh.minutes}") int initialDelay,
                       @Value("${period.policies.refresh.minutes}") int period,
                       PdpPolicyViolationRepository pdpPolicyViolationRepository,
                       PDPEngineHolder pdpEngineHolder) {
    this.pdpEngineHolder = pdpEngineHolder;
    this.pdpEngine = pdpEngineHolder.pdpEngine();
    this.pdpPolicyViolationRepository = pdpPolicyViolationRepository;

    ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    Runnable task = () -> this.refreshPolicies();
    executor.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.MINUTES);
  }

  @RequestMapping(method = RequestMethod.POST, headers = {"content-type=application/json"}, value = "/decide")
  public String decide(@RequestBody String payload) throws Exception {
    long start = System.currentTimeMillis();
    LOG.debug("decide request: {}", payload);

    Request pdpRequest = JSONRequest.load(payload);
    Response pdpResponse;
    try {
      lock.readLock().lock();
      pdpResponse = pdpEngine.decide(pdpRequest);
    } finally {
      lock.readLock().unlock();
    }
    String response = JSONResponse.toString(pdpResponse, LOG.isDebugEnabled());
    LOG.debug("decide response: {} took: {} ms", response, System.currentTimeMillis() - start);


    reportPolicyViolation(pdpResponse, payload);
    return response;
  }

  private void reportPolicyViolation(Response pdpResponse, String payload) {
    Collection<Result> results = pdpResponse.getResults();
    if (!CollectionUtils.isEmpty(results) && results.stream().anyMatch(result -> result.getDecision().equals(DENY))) {
      Collection<Advice> associatedAdvices = results.iterator().next().getAssociatedAdvice();
      String associatedAdviceId = CollectionUtils.isEmpty(associatedAdvices) ?
          "No associated advice present on Policy. Please check all policies and repair those without Deny advice" : associatedAdvices.iterator().next().getId().stringValue();
      pdpPolicyViolationRepository.save(new PdpPolicyViolation(associatedAdviceId, payload));
    }
  }


  private void refreshPolicies() {
    LOG.info("Starting reloading policies");
    long start = System.currentTimeMillis();
    lock.writeLock().lock();
    try {
      this.pdpEngine = pdpEngineHolder.pdpEngine();
    } finally {
      lock.writeLock().unlock();
    }
    LOG.info("Finished reloading policies in {} ms", System.currentTimeMillis() - start);
  }

}
