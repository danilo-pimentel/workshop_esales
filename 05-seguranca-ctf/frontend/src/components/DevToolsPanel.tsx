import { useState } from 'react';
import RequestLog from './RequestLog';
import CustomRequest from './CustomRequest';
import { useRequestStore } from '../store/requestStore';

type Tab = 'log' | 'request';

interface DevToolsPanelProps {
  open: boolean;
  onClose: () => void;
}

export default function DevToolsPanel({ open, onClose }: DevToolsPanelProps) {
  const [activeTab, setActiveTab] = useState<Tab>('log');
  const requests = useRequestStore((s) => s.requests);

  const findingCount = requests.reduce((acc, r) => acc + r.findings.length, 0);

  if (!open) return null;

  return (
    <>
      <div
        className="fixed inset-0 z-40 bg-black/20 backdrop-blur-sm md:hidden"
        onClick={onClose}
      />

      <div
        className="fixed top-0 right-0 bottom-0 z-50 w-full md:w-[620px] lg:w-[720px] bg-gray-900 text-gray-100 flex flex-col shadow-2xl border-l border-gray-700 animate-slide-in"
        style={{ fontFamily: "'Fira Code', 'Cascadia Code', Consolas, monospace" }}
      >
        <div className="flex items-center justify-between px-4 py-2 bg-gray-950 border-b border-gray-700 shrink-0">
          <div className="flex items-center gap-2">
            <span className="text-red-500 select-none">●</span>
            <span className="text-yellow-500 select-none">●</span>
            <span className="text-green-500 select-none">●</span>
            <span className="text-sm font-bold text-gray-300 ml-2">DevTools</span>
            <span className="text-[10px] text-gray-600 ml-1">Ctrl+Shift+D para fechar</span>
          </div>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-300 text-lg leading-none transition-colors"
            title="Fechar"
          >
            ✕
          </button>
        </div>

        <div className="flex border-b border-gray-700 shrink-0">
          <TabButton
            label="Request Log"
            active={activeTab === 'log'}
            onClick={() => setActiveTab('log')}
            badge={requests.length > 0 ? String(requests.length) : undefined}
            badgeAlert={findingCount > 0}
          />
          <TabButton
            label="Custom Request"
            active={activeTab === 'request'}
            onClick={() => setActiveTab('request')}
          />
        </div>

        <div className="flex-1 overflow-hidden">
          {activeTab === 'log' && <RequestLog />}
          {activeTab === 'request' && <CustomRequest />}
        </div>
      </div>
    </>
  );
}

interface TabButtonProps {
  label: string;
  active: boolean;
  onClick: () => void;
  badge?: string;
  badgeAlert?: boolean;
}

function TabButton({ label, active, onClick, badge, badgeAlert }: TabButtonProps) {
  return (
    <button
      onClick={onClick}
      className={`px-4 py-2.5 text-xs font-medium border-b-2 transition-colors flex items-center gap-1.5 ${
        active
          ? 'border-blue-500 text-blue-400 bg-gray-800'
          : 'border-transparent text-gray-500 hover:text-gray-300 hover:bg-gray-800/50'
      }`}
    >
      {label}
      {badge && (
        <span
          className={`text-[10px] px-1.5 py-0.5 rounded-full font-bold ${
            badgeAlert ? 'bg-orange-600 text-white' : 'bg-gray-700 text-gray-300'
          }`}
        >
          {badge}
        </span>
      )}
    </button>
  );
}
