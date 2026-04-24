import {registry} from '@jahia/ui-extender';
import register from './TomcatLogProvider/register';
import i18next from 'i18next';

export default function () {
    registry.add('callback', 'tomcat-log-provider', {
        targets: ['jahiaApp-init:50'],
        callback: async () => {
            await i18next.loadNamespaces('tomcat-log-provider', () => {
                console.debug('%c tomcat-log-provider: i18n namespace loaded', 'color: #006633');
            });
            register();
            console.debug('%c tomcat-log-provider: activation completed', 'color: #006633');
        }
    });
}
