import React, {useState} from 'react';
import {useMutation, useQuery} from '@apollo/client';
import {useTranslation} from 'react-i18next';
import {Button, Loader, Typography} from '@jahia/moonstone';
import styles from './TomcatLogProvider.scss';
import {GET_SETTINGS, SAVE_SETTINGS} from './TomcatLogProvider.gql';

export const TomcatLogProviderAdmin = () => {
    const {t} = useTranslation('tomcat-log-provider');
    const [saveStatus, setSaveStatus] = useState(null);
    const [mountPath, setMountPath] = useState('');

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
    };

    if (loading) {
        return (
            <div className={styles.tlp_loading}>
                <Loader size="big"/>
            </div>
        );
    }

    return (
        <div className={styles.tlp_container}>
            <div className={styles.tlp_header}>
                <h2>{t('label.title')}</h2>
            </div>

            <div className={styles.tlp_description}>
                <Typography>{t('label.description')}</Typography>
            </div>

            <div className={styles.tlp_form}>
                <div className={styles.tlp_fieldGroup}>
                    <label className={styles.tlp_label}>{t('label.logPath')}</label>
                    <span className={styles.tlp_readOnly}>{logPath}</span>
                    <span className={styles.tlp_hint}>{t('label.logPathHint')}</span>
                </div>

                <div className={styles.tlp_fieldGroup}>
                    <label className={styles.tlp_label} htmlFor="tlp-mount-path">
                        {t('label.mountPath')}
                    </label>
                    <input
                        type="text"
                        id="tlp-mount-path"
                        className={styles.tlp_input}
                        value={mountPath}
                        onChange={e => {
                            setMountPath(e.target.value);
                            setSaveStatus(null);
                        }}
                    />
                    <span className={styles.tlp_hint}>{t('label.mountPathHint')}</span>
                </div>
            </div>

            <div className={styles.tlp_actions}>
                {saveStatus === 'success' && (
                    <div className={`${styles.tlp_alert} ${styles['tlp_alert--success']}`}>
                        {t('label.saveSuccess')}
                    </div>
                )}
                {saveStatus === 'error' && (
                    <div className={`${styles.tlp_alert} ${styles['tlp_alert--error']}`}>
                        {t('label.saveError')}
                    </div>
                )}
                <Button
                    label={t('label.save')}
                    variant="primary"
                    isDisabled={saving || !mountPath.trim()}
                    onClick={handleSave}
                />
            </div>
        </div>
    );
};

export default TomcatLogProviderAdmin;
