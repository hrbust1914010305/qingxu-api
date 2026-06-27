-- 用户配置表
CREATE TABLE sys_user_preference (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    
    -- 系统配置（10个字段）
    collapsed BOOLEAN DEFAULT FALSE,
    is_accordion BOOLEAN DEFAULT TRUE,
    is_breadcrumb BOOLEAN DEFAULT TRUE,
    is_tabs BOOLEAN DEFAULT TRUE,
    is_footer BOOLEAN DEFAULT TRUE,
    watermark VARCHAR(255) DEFAULT '',
    watermark_color VARCHAR(32) DEFAULT 'rgba(0, 0, 0, 0.15)',
    watermark_font_size INTEGER DEFAULT 14,
    watermark_rotate INTEGER DEFAULT -20,
    watermark_gap VARCHAR(100) DEFAULT '[100, 100]',
    
    -- 主题配置（7个字段）
    layout_type VARCHAR(64) DEFAULT 'layoutDefaults',
    theme_color VARCHAR(32) DEFAULT '#165DFF',
    color_weak_mode BOOLEAN DEFAULT FALSE,
    gray_mode BOOLEAN DEFAULT FALSE,
    aside_dark BOOLEAN DEFAULT FALSE,
    dark_mode BOOLEAN DEFAULT FALSE,
    transition_page VARCHAR(64) DEFAULT 'fadeInOut',
    
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id)
);

COMMENT ON TABLE sys_user_preference IS '用户配置表';
COMMENT ON COLUMN sys_user_preference.id IS '主键';
COMMENT ON COLUMN sys_user_preference.user_id IS '用户ID';

COMMENT ON COLUMN sys_user_preference.collapsed IS '菜单折叠';
COMMENT ON COLUMN sys_user_preference.is_accordion IS '菜单手风琴模式';
COMMENT ON COLUMN sys_user_preference.is_breadcrumb IS '面包屑是否显示';
COMMENT ON COLUMN sys_user_preference.is_tabs IS '标签页是否显示';
COMMENT ON COLUMN sys_user_preference.is_footer IS '页脚是否显示';
COMMENT ON COLUMN sys_user_preference.watermark IS '水印文字';
COMMENT ON COLUMN sys_user_preference.watermark_color IS '水印颜色';
COMMENT ON COLUMN sys_user_preference.watermark_font_size IS '水印字号';
COMMENT ON COLUMN sys_user_preference.watermark_rotate IS '水印旋转角度';
COMMENT ON COLUMN sys_user_preference.watermark_gap IS '水印间距(JSON数组格式)';

COMMENT ON COLUMN sys_user_preference.layout_type IS '布局类型';
COMMENT ON COLUMN sys_user_preference.theme_color IS '主题色';
COMMENT ON COLUMN sys_user_preference.color_weak_mode IS '色弱模式';
COMMENT ON COLUMN sys_user_preference.gray_mode IS '灰色模式';
COMMENT ON COLUMN sys_user_preference.aside_dark IS '侧边栏独立深色';
COMMENT ON COLUMN sys_user_preference.dark_mode IS '暗黑模式';
COMMENT ON COLUMN sys_user_preference.transition_page IS '页面过渡动画';