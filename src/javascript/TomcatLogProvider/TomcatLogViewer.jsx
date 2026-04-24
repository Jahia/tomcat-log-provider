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
            <div className={styles.tlp_logHeader}>
                <span className={styles.tlp_label}>{t('label.logViewer')}</span>
                {!autoScroll && (
                    <span className={styles.tlp_logPaused}>{t('label.logViewerPaused')}</span>
                )}
            </div>
            <div
                ref={logContainerRef}
                className={`${styles.tlp_logTerminal} ${styles['tlp_logTerminal--fullPage']}`}
                onScroll={handleLogScroll}
            >
                {logLines.map((line, i) => (
                    // eslint-disable-next-line react/no-array-index-key
                    <div key={i} className={styles.tlp_logLine}>{line}</div>
                ))}
                <div ref={logEndRef}/>
            </div>
        </div>
    );
};

export default TomcatLogViewer;
