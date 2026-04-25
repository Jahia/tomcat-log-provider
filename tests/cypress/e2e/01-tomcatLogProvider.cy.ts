import {DocumentNode} from 'graphql';

describe('Tomcat Log Provider', () => {
    const configPath = '/jahia/administration/tomcatLogProvider';
    const logViewerPath = '/jahia/administration/tomcatLogViewer';
    const defaultMountPath = '/sites/systemsite/files/tomcat-logs';

    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const getSettings: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getMountPoints.graphql');
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const saveSettings: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/mutation/addMountPoint.graphql');
    // eslint-disable-next-line @typescript-eslint/no-var-requires
    const getLogFile: DocumentNode = require('graphql-tag/loader!../fixtures/graphql/query/getLogFile.graphql');

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

    // ─── Log file access ─────────────────────────────────────────────────────────

    describe('Log file access', () => {
        const logFilePath = `${defaultMountPath}/jahia.log`;

        before(() => {
            cy.login();
            cy.apollo({
                mutation: saveSettings,
                variables: {mountPath: defaultMountPath}
            });
        });

        it('jahia.log exists as a jnt:file node under the mount point', () => {
            cy.login();
            cy.apollo({query: getLogFile, variables: {path: logFilePath}})
                .its('data.jcr.nodeByPath')
                .should(node => {
                    expect(node).to.not.be.null;
                    expect(node.primaryNodeType.name).to.eq('jnt:file');
                });
        });

        it('jahia.log jcr:content has a text MIME type', () => {
            cy.login();
            cy.apollo({query: getLogFile, variables: {path: logFilePath}})
                .its('data.jcr.nodeByPath.descendant.property.value')
                .should('match', /^text\//);
        });

        it('jahia.log content is not empty', () => {
            cy.login();
            cy.request(`/files/default${logFilePath}`)
                .its('body')
                .should('not.be.empty');
        });
    });

    // ─── Admin UI — Configuration ─────────────────────────────────────────────────

    describe('Admin UI — Configuration', () => {
        it('shows the admin panel title', () => {
            cy.login();
            cy.visit(configPath);
            cy.contains('Tomcat Log Provider').should('be.visible');
        });

        it('shows the mount path input field', () => {
            cy.login();
            cy.visit(configPath);
            cy.get('#tlp-mount-path').should('be.visible');
        });

        it('shows the save button', () => {
            cy.login();
            cy.visit(configPath);
            cy.contains('button', 'Save settings').should('be.visible');
        });

        it('shows success alert after saving via button', () => {
            cy.login();
            cy.visit(configPath);
            cy.get('#tlp-mount-path').clear();
            cy.get('#tlp-mount-path').type('/sites/systemsite/files/tomcat-logs-ui-test');
            cy.contains('button', 'Save settings').click();
            cy.get('[class*="tlp_alert--success"]').should('be.visible');
        });

        it('shows success alert after saving via Ctrl+Enter', () => {
            cy.login();
            cy.visit(configPath);
            cy.get('#tlp-mount-path').clear();
            cy.get('#tlp-mount-path').type('/sites/systemsite/files/tomcat-logs-keyboard-test');
            cy.get('#tlp-mount-path').type('{ctrl+enter}');
            cy.get('[class*="tlp_alert--success"]').should('be.visible');
        });

        it('Ctrl+Enter does nothing when the field is empty', () => {
            cy.login();
            cy.visit(configPath);
            cy.get('#tlp-mount-path').clear();
            cy.get('#tlp-mount-path').type('{ctrl+enter}');
            cy.get('[class*="tlp_alert--success"]').should('not.exist');
        });
    });

    // ─── Admin UI — Browse in jContent ───────────────────────────────────────────

    describe('Admin UI — Browse in jContent', () => {
        it('shows the Browse in jContent button', () => {
            cy.login();
            cy.visit(configPath);
            cy.contains('button', 'Browse in jContent').should('be.visible');
        });

        it('Browse in jContent button is enabled for a valid /sites/ mount path', () => {
            cy.login();
            cy.visit(configPath);
            cy.get('#tlp-mount-path').clear();
            cy.get('#tlp-mount-path').type('/sites/systemsite/files/tomcat-logs');
            cy.contains('button', 'Browse in jContent').should('not.be.disabled');
        });

        it('Browse in jContent button is disabled when mount path is not a /sites/ path', () => {
            cy.login();
            cy.visit(configPath);
            cy.get('#tlp-mount-path').clear();
            cy.get('#tlp-mount-path').type('/invalid/path');
            cy.contains('button', 'Browse in jContent').should('be.disabled');
        });

        it('Browse in jContent button opens the correct jContent URL', () => {
            cy.login();
            cy.visit(configPath);
            cy.window().then(win => {
                cy.stub(win, 'open').as('windowOpen');
            });
            cy.get('#tlp-mount-path').clear();
            cy.get('#tlp-mount-path').type('/sites/systemsite/files/tomcat-logs');
            cy.contains('button', 'Browse in jContent').click();
            cy.get('@windowOpen').should(
                'have.been.calledWith',
                '/jahia/jcontent/systemsite/en/media/files/tomcat-logs',
                '_blank'
            );
        });
    });

    // ─── Admin UI — Log Viewer ────────────────────────────────────────────────────

    describe('Admin UI — Log Viewer', () => {
        it('shows the live tail label', () => {
            cy.login();
            cy.visit(logViewerPath);
            cy.contains('Live tail — jahia.log', {timeout: 10000}).should('be.visible');
        });

        it('shows the log terminal', () => {
            cy.login();
            cy.visit(logViewerPath);
            cy.get('[class*="tlp_logTerminal"]', {timeout: 10000}).should('be.visible');
        });

        it('displays log lines in the terminal', () => {
            cy.login();
            cy.visit(logViewerPath);
            cy.get('[class*="tlp_logLine"]', {timeout: 10000}).should('have.length.greaterThan', 0);
        });
    });
});
