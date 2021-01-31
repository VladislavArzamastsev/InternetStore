package dao.country;

import entity.Country;
import entity.Currency;
import exception.dao.DeleteException;
import exception.dao.FetchException;
import exception.dao.StoreException;
import exception.dao.UpdateException;
import hibernate.HibernateUtils;
import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import org.springframework.stereotype.Repository;

import javax.annotation.concurrent.NotThreadSafe;
import java.util.Collection;
import java.util.Objects;

@Repository("hibernateCountryDao")
@NotThreadSafe
public final class HibernateCountryDao implements CountryDao {


    @Override
    public Country getById(Integer id) throws FetchException {
        Objects.requireNonNull(id);
        Country out;
        try (Session session = HibernateUtils.openSession()) {
            Transaction transaction = session.beginTransaction();
            out = session.get(Country.class, id);
            transaction.commit();
        } catch (HibernateException e) {
            throw new FetchException(e);
        }
        if (out == null) {
            throw new FetchException("No such country with id = " + id);
        }
        return out;
    }

    @Override
    public Collection<Country> allEntities() throws FetchException {
        Collection<Country> out;
        try (Session session = HibernateUtils.openSession()) {
            Transaction transaction = session.beginTransaction();
            out = session.createQuery("SELECT c FROM entity.Country c", Country.class).list();
            transaction.commit();
        } catch (HibernateException e) {
            throw new FetchException(e);
        }
        return out;
    }

    @Override
    public void save(Country country) throws StoreException {
        Objects.requireNonNull(country);
        try (Session session = HibernateUtils.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.save(country);
            transaction.commit();
        } catch (HibernateException e) {
            throw new StoreException(e);
        }
    }

    @Override
    public void saveIgnoreId(Country country) throws StoreException {
        Objects.requireNonNull(country);
        try (Session session = HibernateUtils.openSession()) {
            Transaction transaction = session.beginTransaction();

            Query<?> countryQuery = session.createSQLQuery("INSERT INTO is_country(country_name)" +
                    " VALUES (?)");
            Query<?> countryToCurrencyQuery = session.createSQLQuery("INSERT INTO is_country_to_currency(country_id, currency_id) " +
                    "VALUES ((SELECT country_id FROM is_country WHERE BINARY country_name = ?), " +
                    "(SELECT currency_id FROM is_currency WHERE BINARY currency_name = ?))");
            countryQuery.setParameter(1, country.getCountryName());
            countryToCurrencyQuery.setParameter(1, country.getCountryName());
            try {
                countryQuery.executeUpdate();
                for (Currency currency : country.getCurrencies()) {
                    countryToCurrencyQuery.setParameter(2, currency.getCurrencyName());
                    countryToCurrencyQuery.executeUpdate();
                }
            } catch (HibernateException e) {
                transaction.rollback();
                throw new StoreException(e);
            }
            transaction.commit();
        } catch (HibernateException e) {
            throw new StoreException(e);
        }
    }

    @Override
    @SuppressWarnings("Duplicates")
    public void update(Integer id, Country country) throws UpdateException {
        Objects.requireNonNull(id);
        Objects.requireNonNull(country);
        Integer prevId = country.getId();
        country.setId(id);
        try (Session session = HibernateUtils.openSession()) {
            Transaction transaction = session.beginTransaction();
            session.update(country);
            transaction.commit();
        } catch (HibernateException e) {
            throw new UpdateException(e);
        }finally {
            country.setId(prevId);
        }
    }

    @Override
    public void delete(Integer id) throws DeleteException {
        Objects.requireNonNull(id);
        try (Session session = HibernateUtils.openSession()) {
            Transaction transaction = session.beginTransaction();
            Query<?> query = session.createQuery("DELETE FROM entity.Country c WHERE c.id = :id");
            query.setParameter("id", id);
            query.executeUpdate();
            transaction.commit();
        } catch (HibernateException e) {
            throw new DeleteException(e);
        }
    }

    @Override
    public Country getByName(String countryName) throws FetchException {
        Objects.requireNonNull(countryName);
        Country out;
        try (Session session = HibernateUtils.openSession()) {
            Transaction transaction = session.beginTransaction();
            Query<Country> query = session.createQuery("SELECT c FROM entity.Country c WHERE c.countryName = :name", Country.class);
            query.setParameter("name", countryName);
            out = query.uniqueResult();
            transaction.commit();
        } catch (HibernateException e) {
            throw new FetchException(e);
        }
        if (out == null) {
            throw new FetchException("No such country with name = " + countryName);
        }
        return out;
    }

    @Override
    public void deleteByName(String countryName) throws DeleteException {
        Objects.requireNonNull(countryName);
        try (Session session = HibernateUtils.openSession()) {
            Transaction transaction = session.beginTransaction();
            Query<?> query = session.createQuery("DELETE FROM entity.Country c WHERE c.countryName = :countryName");
            query.setParameter("countryName", countryName);
            query.executeUpdate();
            transaction.commit();
        } catch (HibernateException e) {
            throw new DeleteException(e);
        }
    }

}
