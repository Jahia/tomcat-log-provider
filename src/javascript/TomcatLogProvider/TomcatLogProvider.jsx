import React, {useEffect, useRef, useState} from 'react';
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
    const saveLiveRef = useRef(null);

    useEffect(() => {
        document.title = `${t('label.title')} — Jahia Administration`;
    }, [t]);

    const {loading} = useQuery(GET_SETTINGS, {
        fetchPolicy: 'network-only',
        onCompleted: data => {
            const s = data?.tomcatLogSettings;
            if (s) {
                setMountPath(s.mountPath ?? '');
            }
        }
    });

    const {data} = useQuery(GET_SETTINGS, {fetchPolicy: 'cache-first'});
    const logPath = data?.tomcatLogSettings?.logPath ?? '';
    const jContentUrl = buildJContentUrl(mountPath);

    const [saveSettings, {loading: saving}] = useMutation(SAVE_SETTINGS);

    const handleSave = async () => {
        setSaveStatus(null);
        try {
            const result = await saveSettings({variables: {mountPath}});
            setSaveStatus(result.data?.tomcatLogSaveSettings ? 'success' : 'error');
        } catch (err) {
            console.error('Failed to save settings:', err);
            setSaveStatus('error');
        }

        setTimeout(() => saveLiveRef.current?.focus(), 50);
    };

    const saveLiveMsg = saveStatus === 'success' ? t('label.saveSuccess') :
        saveStatus === 'error' ? t('label.saveError') : '';

    if (loading) {
        return (
            <div className={styles.tlp_loading} role="status">
                <span className={styles.tlp_sr_only}>{t('label.loading')}</span>
                <Loader size="big"/>
            </div>
        );
    }

    return (
        <div className={styles.tlp_container}>
            {/* Persistent live region — always in DOM so AT registers it before status changes */}
            <div
                ref={saveLiveRef}
                tabIndex={-1}
                role={saveStatus === 'error' ? 'alert' : 'status'}
                aria-live={saveStatus === 'error' ? 'assertive' : 'polite'}
                aria-atomic="true"
                className={styles.tlp_sr_only}
            >
                {saveLiveMsg}
            </div>

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
                            aria-describedby="tlp-mount-hint"
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
                            isDisabled={saving || !mountPath.trim()}
                            onClick={handleSave}
                        />
                        <Button
                            label={t('label.browseInJContent')}
                            variant="secondary"
                            isDisabled={!jContentUrl}
                            onClick={() => window.open(jContentUrl, '_blank')}
                        />
                        <span className={styles.tlp_sr_only}>{t('label.opensInNewTab')}</span>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default TomcatLogProviderAdmin;
