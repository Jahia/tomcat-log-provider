import React, {useEffect, useState} from 'react';
import {useMutation, useQuery} from '@apollo/client';
import {useTranslation} from 'react-i18next';
import {Button, Loader, Typography} from '@jahia/moonstone';
import styles from './TomcatLogProvider.scss';
import {GET_SETTINGS, SAVE_SETTINGS} from './TomcatLogProvider.gql';

const buildJContentUrl = jcrPath => {
    const match = jcrPath.match(/^\/sites\/([^/]+)\/files(\/.*)?$/);
    if (!match) {
        return null;
    }

    const siteKey = match[1];
    const rest = match[2] ?? '';
    return `/jahia/jcontent/${siteKey}/en/media/files${rest}`;
};

export const TomcatLogProviderAdmin = () => {
    const {t} = useTranslation('tomcat-log-provider');
    const [saveStatus, setSaveStatus] = useState(null);
    const [mountPath, setMountPath] = useState('');

    useEffect(() => {
        document.title = `${t('label.title')} — Jahia Administration`;
    }, [t]);

    const {loading} = useQuery(GET_SETTINGS, {
        fetchPolicy: 'network-only',
        onCompleted: data => {
            const s = data?.tomcatLog?.settings;
            if (s) {
                setMountPath(s.mountPath ?? '');
            }
        }
    });

    const {data} = useQuery(GET_SETTINGS, {fetchPolicy: 'cache-first'});
    const logPath = data?.tomcatLog?.settings?.logPath ?? '';
    const jContentUrl = buildJContentUrl(mountPath);

    const [saveSettings, {loading: saving}] = useMutation(SAVE_SETTINGS);

    const handleSave = async () => {
        setSaveStatus(null);
        try {
            const result = await saveSettings({variables: {mountPath}});
            setSaveStatus(result.data?.tomcatLog?.saveSettings ? 'success' : 'error');
        } catch (_err) {
            setSaveStatus('error');
        }
    };

    const saveSuccessMsg = saveStatus === 'success' ? t('label.saveSuccess') : '';
    const saveErrorMsg = saveStatus === 'error' ? t('label.saveError') : '';

    if (loading) {
        return (
            <div className={styles.tlp_loading} role="status" aria-live="polite">
                <span className={styles.tlp_sr_only}>{t('label.loading')}</span>
                <Loader size="big" aria-hidden="true"/>
            </div>
        );
    }

    return (
        <div className={styles.tlp_container}>
            {/* Two fixed-role live regions — always in DOM so AT registers them before status changes */}
            <div role="status" aria-live="polite" aria-atomic="true" className={styles.tlp_sr_only}>{saveSuccessMsg}</div>
            <div role="alert" aria-live="assertive" aria-atomic="true" className={styles.tlp_sr_only}>{saveErrorMsg}</div>

            <div className={styles.tlp_formSection}>
                <div className={styles.tlp_header}>
                    <h2>{t('label.title')}</h2>
                </div>

                <div className={styles.tlp_description}>
                    <Typography>{t('label.description')}</Typography>
                </div>

                <div className={styles.tlp_form}>
                    {/* Read-only field: use dl/dt/dd for label/value association without htmlFor */}
                    <dl className={styles.tlp_fieldGroup}>
                        <dt className={styles.tlp_label}>{t('label.logPath')}</dt>
                        <dd className={styles.tlp_readOnly}>{logPath}</dd>
                        <dd className={styles.tlp_hint}>{t('label.logPathHint')}</dd>
                    </dl>

                    <div className={styles.tlp_fieldGroup}>
                        <label className={styles.tlp_label} htmlFor="tlp-mount-path">
                            {t('label.mountPath')}
                        </label>
                        <input
                            type="text"
                            id="tlp-mount-path"
                            className={styles.tlp_input}
                            value={mountPath}
                            required
                            aria-required="true"
                            aria-invalid={saveStatus === 'error' ? 'true' : undefined}
                            aria-describedby="tlp-mount-hint tlp-mount-error"
                            aria-keyshortcuts="Control+Enter"
                            onChange={e => {
                                setMountPath(e.target.value);
                                setSaveStatus(null);
                            }}
                            onKeyDown={e => {
                                if (e.key === 'Enter' && e.ctrlKey && mountPath.trim()) {
                                    handleSave();
                                }
                            }}
                        />
                        <span id="tlp-mount-hint" className={styles.tlp_hint}>{t('label.mountPathHint')}</span>
                        <span id="tlp-mount-error" className={styles.tlp_sr_only}>{saveErrorMsg}</span>
                    </div>
                </div>

                <div className={styles.tlp_actions}>
                    {saveStatus === 'success' && (
                        <div aria-hidden="true" className={`${styles.tlp_alert} ${styles['tlp_alert--success']}`}>
                            <span className={styles.tlp_alertIcon}>✓</span> {t('label.saveSuccess')}
                        </div>
                    )}
                    {saveStatus === 'error' && (
                        <div aria-hidden="true" className={`${styles.tlp_alert} ${styles['tlp_alert--error']}`}>
                            <span className={styles.tlp_alertIcon}>✕</span> {t('label.saveError')}
                        </div>
                    )}
                    <div className={styles.tlp_buttons}>
                        <Button
                            label={t('label.save')}
                            variant="primary"
                            type="button"
                            isDisabled={saving || !mountPath.trim()}
                            onClick={handleSave}
                        />
                        <Button
                            label={t('label.browseInJContent')}
                            variant="secondary"
                            type="button"
                            isDisabled={!jContentUrl}
                            aria-describedby="tlp-browse-newtab-hint"
                            onClick={() => window.open(jContentUrl, '_blank')}
                        />
                        <span id="tlp-browse-newtab-hint" className={styles.tlp_sr_only}>{t('label.opensInNewTab')}</span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TomcatLogProviderAdmin;
