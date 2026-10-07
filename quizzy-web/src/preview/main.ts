// 设计系统预览页的入口：只做一件事——把 token 层挂上。
// ⚠️ 这个入口**不进主应用**（主应用还没迁移，见 docs/adr/0031）；
//    它单独一份，专门用来肉眼看 token 的效果。
import '../styles/tokens.css'
