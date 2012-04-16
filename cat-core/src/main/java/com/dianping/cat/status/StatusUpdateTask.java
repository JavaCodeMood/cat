package com.dianping.cat.status;

import org.codehaus.plexus.personality.plexus.lifecycle.phase.Initializable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.InitializationException;

import com.dianping.cat.Cat;
import com.dianping.cat.configuration.NetworkInterfaceManager;
import com.dianping.cat.message.Message;
import com.dianping.cat.message.MessageProducer;
import com.dianping.cat.message.Transaction;
import com.dianping.cat.message.internal.MilliSecondTimer;
import com.dianping.cat.message.spi.MessageStatistics;
import com.dianping.cat.status.model.entity.StatusInfo;
import com.site.helper.Threads.Task;
import com.site.lookup.annotation.Inject;

public class StatusUpdateTask implements Task, Initializable {
	@Inject
	private MessageStatistics m_statistics;

	private boolean m_active = true;

	private String m_ipAddress;

	private long m_interval = 60 * 1000; // 60 seconds

	@Override
	public String getName() {
		return "StatusUpdateTask";
	}

	@Override
	public void initialize() throws InitializationException {
		m_ipAddress = NetworkInterfaceManager.INSTANCE.getLocalHostAddress();
	}

	@Override
	public void run() {
		Cat.setup("StatusUpdateTask");

		while (m_active) {
			long start = MilliSecondTimer.currentTimeMillis();
			MessageProducer cat = Cat.getProducer();
			Transaction t = cat.newTransaction("Task", "Status");
			StatusInfo status = new StatusInfo();

			status.accept(new StatusInfoCollector(m_statistics));
			cat.logHeartbeat("Heartbeat", m_ipAddress, Message.SUCCESS, status.toString());
			t.setStatus(Message.SUCCESS);
			t.complete();

			long elapsed = MilliSecondTimer.currentTimeMillis() - start;

			if (elapsed < m_interval) {
				try {
					Thread.sleep(m_interval - elapsed);
				} catch (InterruptedException e) {
					break;
				}
			}
		}

		Cat.reset();
	}

	public void setInterval(long interval) {
		m_interval = interval;
	}

	@Override
	public void shutdown() {
		m_active = false;
	}
}
