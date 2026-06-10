import {DocumentNode} from 'graphql';
import {createUser, deleteUser, grantRoles} from '@jahia/cypress';

/**
 * Regression tests for the fine-grained `tomcatLogProviderAdmin` permission.
 *
 * These guard against the gate being silently removed or mismatched across the stack:
 *  - Backend: `@GraphQLRequiresPermission("tomcatLogProviderAdmin")` is enforced as
 *    `session.getNode("/").hasPermission("tomcatLogProviderAdmin")` (root-node ACL check).
 *    The data source's `getPrivilegesNames()` was also relaxed from a hardcoded `admin`
 *    check to `tomcatLogProviderAdmin`, so the role works end-to-end.
 *  - Frontend: `requiredPermission: 'tomcatLogProviderAdmin'` in register.jsx gates the admin routes.
 *  - RBAC content: the module ships the assignable `tomcat-log-provider-administrator` role
 *    (src/main/import/roles.xml) granting ONLY that permission (plus `administrationAccess`).
 *
 * The "allowed" user is granted that role and nothing else — never `admin` — so the tests prove
 * fine-grained granularity, not merely that a full administrator can pass.
 */
describe('Tomcat Log Provider — permission enforcement', () => {
    const ROLE_NAME = 'tomcat-log-provider-administrator';
    const DENIED_USER = 'tlpDeniedUser';
    const ALLOWED_USER = 'tlpAllowedUser';
    const PASSWORD = 'TlpPerm9PwdTest';
    const ADMIN_PATH = '/jahia/administration/tomcatLogProvider';

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const getSettings: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getMountPoints.graphql');

    const errorsOf = (result: {graphQLErrors?: Array<{message: string}>; errors?: Array<{message: string}>}) =>
        result.graphQLErrors ?? result.errors ?? [];

    const querySettingsAs = (username: string) => {
        cy.apolloClient({username, password: PASSWORD});
        return cy.apollo({query: getSettings});
    };

    before(() => {
        cy.login();
        createUser(DENIED_USER, PASSWORD);
        createUser(ALLOWED_USER, PASSWORD);
        // The annotation resolves the permission on the JCR root node, so grant the
        // module-shipped single-permission role on `/`.
        grantRoles('/', [ROLE_NAME], ALLOWED_USER, 'USER');
    });

    after(() => {
        cy.apolloClient(); // reset the current Apollo client back to root
        cy.login();
        deleteUser(DENIED_USER);
        deleteUser(ALLOWED_USER);
    });

    describe('GraphQL API authorization', () => {
        it('denies the gated query for a user without the permission', () => {
            querySettingsAs(DENIED_USER).then((result: never) => {
                const errs = errorsOf(result);
                expect(errs, 'denial errors').to.have.length.greaterThan(0);
                expect(errs.map((e: {message: string}) => e.message).join(' ')).to.contain('Permission denied');
            });
        });

        it('allows the gated query for a user granted only the module permission', () => {
            querySettingsAs(ALLOWED_USER).then((result: never) => {
                expect(errorsOf(result), 'should have no errors').to.have.length(0);
                const settings = (result as {data: {tomcatLogSettings: {mountPath: string; logPath: string}}}).data.tomcatLogSettings;
                expect(settings).to.have.property('mountPath');
                expect(settings).to.have.property('logPath');
            });
        });
    });

    describe('Admin UI authorization', () => {
        it('hides the admin panel from a user without the permission', () => {
            cy.login(DENIED_USER, PASSWORD);
            cy.visit(ADMIN_PATH, {failOnStatusCode: false});
            cy.contains('Tomcat Log Provider').should('not.exist');
        });

        it('shows the admin panel to a user granted only the module permission', () => {
            cy.login(ALLOWED_USER, PASSWORD);
            cy.visit(ADMIN_PATH);
            cy.contains('Tomcat Log Provider').should('be.visible');
        });
    });
});
