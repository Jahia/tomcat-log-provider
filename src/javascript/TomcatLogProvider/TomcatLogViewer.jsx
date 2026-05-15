import React, {useEffect, useMemo, useRef, useState} from 'react';
import {useQuery} from '@apollo/client';
import {useTranslation} from 'react-i18next';
import styles from './TomcatLogProvider.scss';
import {GET_LOG_TAIL} from './TomcatLogProvider.gql';

export const TomcatLogViewer = () => {
    const {t} = useTranslation('tomcat-log-provider');
    const logEndRef = useRef(null);
    const logContainerRef = useRef(null);
    const [autoScroll, setAutoScroll] = useState(true);

    useEffect(() => {
        document.title = `${t('label.logViewer')} — Jahia Administration`;
    }, [t]);

    const {data: tailData} = useQuery(GET_LOG_TAIL, {
        fetchPolicy: 'network-only',
        pollInterval: 2000
    });
    const logLines = useMemo(() => tailData?.tomcatLogTail ?? [], [tailData]);

    useEffect(() => {
        if (autoScroll && logEndRef.current) {
            logEndRef.current.scrollIntoView({behavior: 'instant'});
        }
    }, [logLines, autoScroll]);

    const handleLogScroll = () => {
        const el = logContainerRef.current;
        if (el) {
            const atBottom = el.scrollHeight - el.scrollTop <= el.clientHeight + 20;
            setAutoScroll(atBottom);
        }
    };

    return (
        <div className={styles.tlp_container}>
            {/* Always-in-DOM sr-only live region — AT must register it before content changes fire */}
            <span role="status" aria-live="polite" className={styles.tlp_sr_only}>{!autoScroll ? t('label.logViewerPaused') : ''}</span>
            <div className={styles.tlp_logHeader}>
                <h2 className={styles.tlp_label}>{t('label.logViewer')}</h2>
                {!autoScroll && (
                    <span aria-hidden="true" className={styles.tlp_logPaused}>{t('label.logViewerPaused')}</span>
                )}
            </div>
            {/* role="region" — avoids VoiceOver/Safari overriding aria-live="off" on role="log",
                which caused every new log line to be announced (MAJ-04) */}
            <div
                ref={logContainerRef}
                role="region"
                aria-label={t('label.logViewer')}
                tabIndex={0}
                className={`${styles.tlp_logTerminal} ${styles['tlp_logTerminal--fullPage']}`}
                onScroll={handleLogScroll}
            >
                {logLines.map((line, i) => (
                    // eslint-disable-next-line react/no-array-index-key
                    <div key={i} className={styles.tlp_logLine}>{line}</div>
                ))}
                <div ref={logEndRef} aria-hidden="true"/>
            </div>
        </div>
    );
};

export default TomcatLogViewer;
