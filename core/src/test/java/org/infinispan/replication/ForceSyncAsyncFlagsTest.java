package org.infinispan.replication;

import org.infinispan.AdvancedCache;
import org.infinispan.commands.remote.CacheRpcCommand;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.context.Flag;
import org.infinispan.remoting.rpc.ResponseFilter;
import org.infinispan.remoting.rpc.ResponseMode;
import org.infinispan.remoting.rpc.RpcManager;
import org.infinispan.remoting.rpc.RpcManagerImpl;
import org.infinispan.remoting.transport.Address;
import org.infinispan.remoting.transport.Transport;
import org.infinispan.test.MultipleCacheManagersTest;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.CleanupAfterMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

/**
 * Tests for FORCE_ASYNCHRONOUS and FORCE_SYNCHRONOUS flags.
 *
 * @author Tomas Sykora
 * @author anistor@redhat.com
 */
@Test(testName = "replication.ForceSyncAsyncFlagsTest", groups = "functional")
@CleanupAfterMethod
public class ForceSyncAsyncFlagsTest extends MultipleCacheManagersTest {

   @Override
   protected void createCacheManagers() {
      // each test will create the needed caches
   }

   public void testForceAsyncFlagUsage() throws Exception {
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.REPL_SYNC, false);
      createClusteredCaches(2, "replSync", builder);

      AdvancedCache cache1 = cache(0, "replSync").getAdvancedCache();
      AdvancedCache cache2 = cache(1, "replSync").getAdvancedCache();

      Transport originalTransport = TestingUtil.extractGlobalComponent(cache1.getCacheManager(), Transport.class);
      RpcManagerImpl rpcManager = (RpcManagerImpl) TestingUtil.extractComponent(cache1, RpcManager.class);

      Transport mockTransport = spy(originalTransport);
      rpcManager.setTransport(mockTransport);

      // check that the replication call was sync
      cache1.put("k", "v");
      verify(mockTransport).invokeRemotely((List<Address>) anyObject(),
            (CacheRpcCommand) anyObject(), eq(ResponseMode.SYNCHRONOUS), anyLong(),
            anyBoolean(), (ResponseFilter) anyObject(), anyBoolean(), anyBoolean());

      reset(mockTransport);

      // verify FORCE_ASYNCHRONOUS flag on SYNC cache
      cache1.withFlags(Flag.FORCE_ASYNCHRONOUS).put("k", "v");
      verify(mockTransport).invokeRemotely((List<Address>) anyObject(),
            (CacheRpcCommand) anyObject(), eq(ResponseMode.ASYNCHRONOUS_WITH_SYNC_MARSHALLING), anyLong(),
            anyBoolean(), (ResponseFilter) anyObject(), anyBoolean(), anyBoolean());
   }

   public void testForceSyncFlagUsage() throws Exception {
      ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.REPL_ASYNC, false);
      builder.clustering().async().asyncMarshalling(true);
      createClusteredCaches(2, "replAsync", builder);

      AdvancedCache cache1 = cache(0, "replAsync").getAdvancedCache();
      AdvancedCache cache2 = cache(1, "replAsync").getAdvancedCache();

      Transport originalTransport = TestingUtil.extractGlobalComponent(cache1.getCacheManager(), Transport.class);
      RpcManagerImpl rpcManager = (RpcManagerImpl) TestingUtil.extractComponent(cache1, RpcManager.class);

      Transport mockTransport = spy(originalTransport);
      rpcManager.setTransport(mockTransport);

      cache1.put("k", "v");
      verify(mockTransport).invokeRemotely((List<Address>) anyObject(),
                                           (CacheRpcCommand) anyObject(), eq(ResponseMode.ASYNCHRONOUS), anyLong(),
                                           anyBoolean(), (ResponseFilter) anyObject(), anyBoolean(), anyBoolean());
      reset(mockTransport);

      // verify FORCE_SYNCHRONOUS flag on ASYNC cache
      cache1.withFlags(Flag.FORCE_SYNCHRONOUS).put("k", "v");
      verify(mockTransport).invokeRemotely((List<Address>) anyObject(),
                                           (CacheRpcCommand) anyObject(), eq(ResponseMode.SYNCHRONOUS), anyLong(),
                                           anyBoolean(), (ResponseFilter) anyObject(), anyBoolean(), anyBoolean());
   }
}
