/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.queue.api.dao.impl;

import static org.hibernate.criterion.Restrictions.eq;

import javax.validation.constraints.NotNull;

import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Optional;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.openmrs.Auditable;
import org.openmrs.ConceptName;
import org.openmrs.OpenmrsObject;
import org.openmrs.Retireable;
import org.openmrs.Voidable;
import org.openmrs.module.queue.api.dao.BaseQueueDao;

@Slf4j
@Getter
@Setter(AccessLevel.MODULE)
@SuppressWarnings("unchecked")
public class AbstractBaseQueueDaoImpl<Q extends OpenmrsObject & Auditable> implements BaseQueueDao<Q> {
	
	private final SessionFactory sessionFactory;
	
	private final Class<Q> clazz;
	
	public AbstractBaseQueueDaoImpl(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
		this.clazz = (Class<Q>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}
	
	protected Session getCurrentSession() {
		return this.getSessionFactory().getCurrentSession();
	}
	
	@Override
	public Optional<Q> get(int id) {
		return Optional.ofNullable((Q) getCurrentSession().get(this.clazz, id));
	}
	
	@Override
	public Optional<Q> get(@NotNull String uuid) {
		Criteria criteria = getCurrentSession().createCriteria(getClazz());
		includeVoidedObjects(criteria, false);
		criteria.add(eq("uuid", uuid)).uniqueResult();
		return Optional.ofNullable((Q) criteria.add(eq("uuid", uuid)).uniqueResult());
	}
	
	@Override
	public Q createOrUpdate(Q queue) {
		this.getCurrentSession().saveOrUpdate(queue);
		return queue;
	}
	
	@Override
	public void delete(Q queue) {
		this.getCurrentSession().delete(queue);
	}
	
	@Override
	public void delete(@NotNull String uuid) {
		this.get(uuid).ifPresent(this::delete);
	}
	
	@Override
	public Collection<Q> findAll() {
		return this.findAll(false);
	}
	
	@Override
	public Collection<Q> findAll(boolean includeVoided) {
		Criteria criteria = getCurrentSession().createCriteria(clazz);
		includeVoidedObjects(criteria, includeVoided);
		return criteria.list();
	}
	
	protected boolean isVoidable() {
		return Voidable.class.isAssignableFrom(clazz);
	}
	
	protected boolean isRetireable() {
		return Retireable.class.isAssignableFrom(clazz);
	}
	
	protected void handleVoidable(Criteria criteria) {
		criteria.add(eq("voided", false));
	}
	
	protected void handleRetireable(Criteria criteria) {
		criteria.add(eq("retired", false));
	}
	
	protected void includeVoidedObjects(Criteria criteria, boolean includeRetired) {
		if (!includeRetired) {
			if (isVoidable()) {
				handleVoidable(criteria);
			} else if (isRetireable()) {
				handleRetireable(criteria);
			}
		}
	}
	
	/**
	 * Creates concept names subQuery
	 *
	 * @param conceptName Concept name
	 * @return {@link DetachedCriteria} conceptName subQuery
	 */
	protected DetachedCriteria conceptByNameDetachedCriteria(@NotNull String conceptName) {
		return DetachedCriteria.forClass(ConceptName.class, "cn").add(Restrictions.eq("cn.name", conceptName))
		        .setProjection(Projections.property("cn.concept"));
	}
}
