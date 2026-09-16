/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api;

import java.util.List;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.openmrs.Visit;
import org.openmrs.api.context.Context;
import org.openmrs.module.queue.api.search.QueueEntrySearchCriteria;
import org.openmrs.module.queue.model.QueueEntry;
import org.openmrs.module.queue.utils.PrivilegeConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Purges the queue entries of a visit before {@link org.openmrs.api.VisitService#purgeVisit}
 * deletes it. Core's purge cascade knows nothing about queue entries, so without this the delete
 * fails on the queue_entry foreign key to visit.
 * <p>
 * Purging cannot be done from a handler. RequiredDataAdvice dispatches handlers only for method
 * names beginning save, create, void, unvoid, retire or unretire, so a purge reaches none of them,
 * and the module hooks the service instead. Voiding is owned by
 * {@link VisitWithQueueEntriesVoidHandler}, which core does reach on the {@code voidVisit} path.
 * <p>
 * The cascade and core's own delete run in one transaction, taken out here, because the entries
 * must not go without the visit. {@code purgeVisit} refuses a visit that still has encounters, and
 * it does so after this advice has run, so the two have to stand or fall together. Propagation is
 * the default, so this joins a caller's transaction where there is one and starts its own where
 * there is not.
 * <p>
 * It has to be taken out here rather than inherited from the service, and the reason is easy to get
 * backwards. {@code applicationContext-service.xml} does declare {@code visitService} as a
 * {@code TransactionProxyFactoryBean} (line 515), but that proxy's own transaction advisor is inert
 * for {@code purgeVisit}: its target is the proxy that the {@code DefaultAdvisorAutoProxyCreator}
 * and {@code TransactionAttributeSourceAdvisor} pair (lines 46 and 52) has already wrapped around
 * {@code visitServiceTarget}, and that target is a JDK proxy of the {@code VisitService} interface,
 * which carries no {@code @Transactional} for the attribute source to find. The transaction
 * therefore begins in the inner proxy, a layer below where {@code Context.addAdvice} appends this
 * advice, and without the one taken out here nothing the cascade did would be rolled back with the
 * refused delete.
 */
public class VisitWithQueueEntriesDeleteAdvice implements MethodInterceptor {
	
	private static final Logger log = LoggerFactory.getLogger(VisitWithQueueEntriesDeleteAdvice.class);
	
	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable {
		Object[] args = invocation.getArguments();
		if (!"purgeVisit".equals(invocation.getMethod().getName()) || args.length == 0 || !(args[0] instanceof Visit)) {
			return invocation.proceed();
		}
		Visit visit = (Visit) args[0];
		if (visit.getVisitId() == null) {
			return invocation.proceed();
		}
		
		// Looked up per call, not held in a field: ModuleUtil and DispatcherServlet re-run
		// ModuleFactory.loadAdvice after a Spring refresh, and AdvicePoint hands them this same cached
		// instance, so a field would outlive the context the bean came from
		PlatformTransactionManager transactionManager = Context.getRegisteredComponent("transactionManager",
		    PlatformTransactionManager.class);
		TransactionStatus transaction = transactionManager.getTransaction(new DefaultTransactionDefinition());
		Object result;
		try {
			purgeQueueEntries(visit);
			result = invocation.proceed();
		}
		catch (Throwable t) {
			try {
				transactionManager.rollback(transaction);
			}
			catch (RuntimeException | Error rollbackFailure) {
				// keep hold of t: it is the only thing that says why the purge was refused, and the
				// rollback failure is what the caller is about to see instead
				rollbackFailure.addSuppressed(t);
				throw rollbackFailure;
			}
			throw t;
		}
		transactionManager.commit(transaction);
		return result;
	}
	
	private void purgeQueueEntries(Visit visit) {
		// Purging a visit is driven by a core service whose callers need not hold queue privileges,
		// so grant them for the duration of this cascade, as the queue handlers do
		Context.addProxyPrivilege(PrivilegeConstants.GET_QUEUE_ENTRIES);
		Context.addProxyPrivilege(PrivilegeConstants.PURGE_QUEUE_ENTRIES);
		try {
			QueueEntryService queueEntryService = Context.getService(QueueEntryService.class);
			QueueEntrySearchCriteria criteria = new QueueEntrySearchCriteria();
			criteria.setVisit(visit);
			// voided entries hold the same foreign key, and a voided patient hides them from the default search
			criteria.setIncludedVoided(true);
			List<QueueEntry> queueEntries = queueEntryService.getQueueEntries(criteria);
			if (!queueEntries.isEmpty()) {
				log.debug("Purging {} queue entries of visit {} being purged", queueEntries.size(), visit.getVisitId());
			}
			for (QueueEntry qe : queueEntries) {
				queueEntryService.purgeQueueEntry(qe);
				log.trace("Purged queue entry {}", qe);
			}
		}
		finally {
			Context.removeProxyPrivilege(PrivilegeConstants.GET_QUEUE_ENTRIES);
			Context.removeProxyPrivilege(PrivilegeConstants.PURGE_QUEUE_ENTRIES);
		}
	}
}
