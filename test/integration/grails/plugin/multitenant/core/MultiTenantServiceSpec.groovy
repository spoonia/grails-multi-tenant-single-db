package grails.plugin.multitenant.core

import demo.DemoAnimal
import demo.DemoProduct
import demo.DemoTenant
import grails.test.spock.IntegrationSpec

/**
 * @author Kim A. Betti
 */
class MultiTenantServiceSpec extends IntegrationSpec {

    DemoTenant testTenant
    MultiTenantService multiTenantService

    def setup() {
        testTenant = new DemoTenant(name: "test tenant", domain: "test.com")
        testTenant.save flush: true, failOnError: true
    }

    def "checked exceptions should roll back transaction"() {
        given:
        DemoProduct product = null
        multiTenantService.doWithTenantId(123) {
            product = new DemoProduct(name: "Some product")
            product.save flush: true, failOnError: true
        }

        when:
        multiTenantService.doWithTenantId(123) {
            product.name = "Another name"
            product.save failOnError: true, flush: true
            throw new Exception("Should cause exception rollback")
        }

        then:
        Exception ex = thrown()

        and:
        product.refresh()
        product.name == "Some product"
    }

    def "unchecked exception should also roll back exception"() {
        given:
        DemoProduct product = null
        multiTenantService.doWithTenantId(123) {
            product = new DemoProduct(name: "Another product")
            product.save flush: true, failOnError: true
        }

        when:
        multiTenantService.doWithTenantId(123) {
            product.name = "Another name"
            product.save failOnError: true, flush: true
            throw new RuntimeException("Should cause exception rollback")
        }

        then:
        RuntimeException ex = thrown()

        and:
        product.refresh()
        product.name == "Another product"
    }

    def "do without tenant restrictions"() {
        given: "we create an animal"
        Tenant.withTenantId(123) {
            new DemoAnimal(name: "Pluto").save(failOnError: true)
        }

        expect: "other tenants cant see it"
        !Tenant.withTenantId(321) {
            DemoAnimal.findByName("Pluto")
        }

        and: "but it should be visible without tenant restrictions"
        Tenant.withoutTenantRestriction {
            DemoAnimal.findByName("Pluto")
        }
    }
}
