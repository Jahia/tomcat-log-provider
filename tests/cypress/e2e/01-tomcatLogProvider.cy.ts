import {DocumentNode} from 'graphql';

describe('Tomcat Log Provider', () => {
    const adminPath = '/jahia/administration/tomcatLogProvider';
    const defaultMountPath = '/sites/systemsite/files/tomcat-logs';

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const getSettings: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getMountPoints.graphql');
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const saveSettings: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/addMountPoint.graphql');

    before(() => {
        cy.login();
    });

    after(() => {
        cy.apollo({
            mutation: saveSettings,
            variables: {mountPath: defaultMountPath}
        });
    });

    // ─── GraphQL API ─────────────────────────────────────────────────────────────

    describe('GraphQL API', () => {
        it('returns settings fields via query', () => {
            cy.apollo({query: getSettings})
                .its('data.tomcatLogSettings')
                .should(s => {
                    expect(s).to.have.property('mountPath');
                    expect(s).to.have.property('logPath');
                });
        });

        it('mountPath is a non-empty JCR path', () => {
            cy.apollo({query: getSettings})
                .its('data.tomcatLogSettings.mountPath')
                .should('match', /^\/.+/);
        });

        it('logPath contains "logs"', () => {
            cy.apollo({query: getSettings})
                .its('data.tomcatLogSettings.logPath')
                .should('include', 'logs');
        });

        it('saves settings and returns true', () => {
            cy.apollo({
                mutation: saveSettings,
                variables: {mountPath: '/sites/systemsite/files/tomcat-logs-test'}
            })
                .its('data.tomcatLogSaveSettings')
                .should('eq', true);
        });

        it('saves settings and reads them back consistently', () => {
            const testPath = '/sites/systemsite/files/tomcat-logs-roundtrip';
            cy.apollo({
                mutation: saveSettings,
                variables: {mountPath: testPath}
            });
            cy.apollo({query: getSettings})
                .its('data.tomcatLogSettings.mountPath')
                .should('eq', testPath);
        });
    });

    // ─── Admin UI ────────────────────────────────────────────────────────────────

    describe('Admin UI', () => {
        it('shows the admin panel title', () => {
            cy.login();
            cy.visit(adminPath);
            cy.contains('Tomcat Log Provider').should('be.visible');
        });

        it('shows the mount path input field', () => {
            cy.login();
            cy.visit(adminPath);
            cy.get('#tlp-mount-path').should('be.visible');
        });

        it('shows the save button', () => {
            cy.login();
            cy.visit(adminPath);
            cy.contains('button', 'Save settings').should('be.visible');
        });

        it('shows a read-only Tomcat log directory path', () => {
            cy.login();
            cy.visit(adminPath);
            cy.get('[class*="tlp_readOnly"]').should('be.visible').and('include.text', 'logs');
        });

        it('shows success alert after saving', () => {
            cy.login();
            cy.visit(adminPath);
            cy.get('#tlp-mount-path').clear();
            cy.get('#tlp-mount-path').type('/sites/systemsite/files/tomcat-logs-ui-test');
            cy.contains('button', 'Save settings').click();
            cy.get('[class*="tlp_alert--success"]').should('be.visible');
        });
    });
});
